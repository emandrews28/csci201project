package service;

import dao.RankingDAO;
import dao.ReviewDAO;
import db.DBConnectionManager;
import model.MapFilterParams;
import model.MapResult;
import model.RankingEntry;
import model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * MapService for the current repo structure.
 *
 * Assumptions:
 * - restaurants table contains:
 *   restaurant_id, name, latitude, longitude, avg_rating, review_count
 * - reviews table contains ranking_score
 * - rankings table contains rank_position
 *
 * Notes:
 * - cuisine / price / openNow are intentionally ignored here because the current repo schema
 *   does not expose those columns yet.
 * - Personalized scoring is based on the current user's rankings and reviews.
 */
public class MapService {
    private static final double EARTH_RADIUS_MILES = 3958.8;
    private static final double MAX_RADIUS_MILES = 25.0;

    private final RankingDAO rankingDAO = new RankingDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    public List<MapResult> getNearbyRestaurants(MapFilterParams filters, Long userId) {
        if (filters == null) {
            return Collections.emptyList();
        }

        Double userLat = filters.getUserLat();
        Double userLng = filters.getUserLng();
        Double radiusMiles = filters.getRadiusMiles();
        Double minRank = filters.getMinRank();

        if (radiusMiles != null && radiusMiles > MAX_RADIUS_MILES) {
            radiusMiles = MAX_RADIUS_MILES;
        }

        Map<Long, Double> personalBoost = (userId == null) ? Collections.emptyMap() : buildPersonalBoost(userId);
        List<MapResult> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT restaurant_id, name, latitude, longitude,
                   COALESCE(avg_rating, 0) AS avg_rating,
                   COALESCE(review_count, 0) AS review_count
            FROM restaurants
            WHERE latitude IS NOT NULL
              AND longitude IS NOT NULL
            """);

        List<Object> params = new ArrayList<>();

        // Coarse bounding box to reduce rows when location is known.
        if (userLat != null && userLng != null && radiusMiles != null && radiusMiles > 0) {
            double latDelta = radiusMiles / 69.0;
            double lngDelta = radiusMiles / Math.max(1e-9, (69.0 * Math.cos(Math.toRadians(userLat))));

            sql.append(" AND latitude BETWEEN ? AND ? ");
            sql.append(" AND longitude BETWEEN ? AND ? ");
            params.add(userLat - latDelta);
            params.add(userLat + latDelta);
            params.add(userLng - lngDelta);
            params.add(userLng + lngDelta);
        }

        sql.append(" ORDER BY COALESCE(avg_rating, 0) DESC, review_count DESC, restaurant_id ASC ");

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object value = params.get(i);
                if (value instanceof Double d) {
                    stmt.setDouble(i + 1, d);
                } else if (value instanceof Integer n) {
                    stmt.setInt(i + 1, n);
                } else if (value instanceof Long l) {
                    stmt.setLong(i + 1, l);
                } else {
                    stmt.setObject(i + 1, value);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    long restaurantId = rs.getLong("restaurant_id");
                    String name = rs.getString("name");
                    double latitude = rs.getDouble("latitude");
                    double longitude = rs.getDouble("longitude");
                    double avgRating = rs.getDouble("avg_rating");
                    int reviewCount = rs.getInt("review_count");

                    double distanceMiles = Double.NaN;
                    if (userLat != null && userLng != null) {
                        distanceMiles = computeDistanceMiles(userLat, userLng, latitude, longitude);
                        if (radiusMiles != null && distanceMiles > radiusMiles) {
                            continue;
                        }
                    }

                    double finalScore = clampScore(avgRating + personalBoost.getOrDefault(restaurantId, 0.0));

                    if (minRank != null && finalScore < minRank) {
                        continue;
                    }

                    MapResult result = new MapResult();
                    result.setRestaurantId(restaurantId);
                    result.setName(name);
                    result.setLatitude(latitude);
                    result.setLongitude(longitude);
                    result.setRankScore(finalScore);
                    result.setDistanceMiles(distanceMiles);

                    results.add(result);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }

        results.sort((a, b) -> {
            int byScore = Double.compare(b.getRankScore(), a.getRankScore());
            if (byScore != 0) return byScore;

            boolean aHasDistance = !Double.isNaN(a.getDistanceMiles());
            boolean bHasDistance = !Double.isNaN(b.getDistanceMiles());
            if (aHasDistance && bHasDistance) {
                int byDistance = Double.compare(a.getDistanceMiles(), b.getDistanceMiles());
                if (byDistance != 0) return byDistance;
            } else if (aHasDistance) {
                return -1;
            } else if (bHasDistance) {
                return 1;
            }

            return a.getName().compareToIgnoreCase(b.getName());
        });

        return results;
    }

    private Map<Long, Double> buildPersonalBoost(long userId) {
        Map<Long, Double> boostByRestaurant = new HashMap<>();

        List<RankingEntry> rankings = rankingDAO.findByUser(userId);
        int rankedCount = rankings.size();

        if (rankedCount > 0) {
            for (RankingEntry entry : rankings) {
                // Higher-ranked restaurants get a larger boost.
                double normalized = (rankedCount - entry.getRankPosition() + 1.0) / rankedCount;
                double boost = normalized * 1.5;
                boostByRestaurant.merge(entry.getRestaurantId(), boost, Double::sum);
            }
        }

        List<Review> reviews = reviewDAO.findByUser(userId);
        for (Review review : reviews) {
            double boost = (review.getRankingScore() / 10.0) * 0.75;
            boostByRestaurant.merge(review.getRestaurantId(), boost, Double::sum);
        }

        return boostByRestaurant;
    }

    private double computeDistanceMiles(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2.0)
                * Math.sin(dLng / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_MILES * c;
    }

    private double clampScore(double score) {
        if (score < 0.0) return 0.0;
        if (score > 10.0) return 10.0;
        return score;
    }
}
