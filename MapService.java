package service;

import dao.RankingDAO;
import dao.RestaurantDAO;
import model.BoundingBox;
import model.MapCluster;
import model.MapFilterParams;
import model.MapResult;
import model.RankingEntry;
import model.Restaurant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapService {

    private final RestaurantDAO restaurantDAO = new RestaurantDAO();
    private final RankingDAO rankingDAO = new RankingDAO();

    public List<MapResult> getNearbyRestaurants(MapFilterParams filters, Long userId) {
        if (filters == null || !filters.validate()) return List.of();

        double radius = filters.getRadiusMiles() == null ? 25.0 : filters.getRadiusMiles();
        double centerLat = filters.getUserLat();
        double centerLng = filters.getUserLng();

        List<Restaurant> restaurants;
        BoundingBox bounds = filters.getBounds();
        if (bounds != null) {
            restaurants = restaurantDAO.findWithinBounds(bounds.getNorth(), bounds.getSouth(), bounds.getEast(), bounds.getWest());
        } else {
            restaurants = restaurantDAO.findNearby(centerLat, centerLng, radius);
        }

        Map<Long, Double> personalizedScores = userId == null ? Map.of() : buildPersonalizedScores(userId);
        List<MapResult> results = new ArrayList<>();

        for (Restaurant restaurant : restaurants) {
            if (restaurant.getLatitude() == null || restaurant.getLongitude() == null) continue;

            double distance = computeDistance(centerLat, centerLng, restaurant.getLatitude(), restaurant.getLongitude());
            if (distance > radius) continue;
            if (!matchesCuisine(filters, restaurant)) continue;
            if (!matchesPriceTier(filters, restaurant)) continue;
            if (!matchesOpenNow(filters, restaurant)) continue;

            double rankScore = computeRankScore(restaurant, personalizedScores.get(restaurant.getRestaurantId()));
            if (filters.getMinRank() != null && rankScore < filters.getMinRank()) continue;

            MapResult result = new MapResult();
            result.setRestaurantId(restaurant.getRestaurantId());
            result.setName(restaurant.getName());
            result.setLatitude(restaurant.getLatitude());
            result.setLongitude(restaurant.getLongitude());
            result.setRankScore(rankScore);
            result.setDistanceMiles(distance);
            result.setCuisine(restaurant.getCuisine());
            result.setPriceTier(restaurant.getPriceTier());
            result.setAddress(restaurant.getAddress());
            result.setOpenNow(restaurant.getOpenNow());
            results.add(result);
        }

        results.sort(Comparator.comparingDouble(MapResult::getRankScore).reversed()
                .thenComparingDouble(MapResult::getDistanceMiles)
                .thenComparing(MapResult::getName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return results;
    }

    public List<MapResult> applyFilters(List<MapResult> restaurants, MapFilterParams filters) {
        if (restaurants == null || filters == null) return List.of();
        List<MapResult> filtered = new ArrayList<>();
        for (MapResult restaurant : restaurants) {
            if (filters.getMinRank() != null && restaurant.getRankScore() < filters.getMinRank()) continue;
            if (filters.getPriceTiers() != null && !filters.getPriceTiers().isEmpty()) {
                Integer tier = restaurant.getPriceTier();
                if (tier == null || !filters.getPriceTiers().contains(tier)) continue;
            }
            if (filters.getCuisines() != null && !filters.getCuisines().isEmpty()) {
                String cuisine = restaurant.getCuisine();
                if (cuisine == null || filters.getCuisines().stream().noneMatch(value -> value != null && value.equalsIgnoreCase(cuisine))) continue;
            }
            filtered.add(restaurant);
        }
        return filtered;
    }

    public double computeDistance(double lat1, double lng1, double lat2, double lng2) {
        final double earthRadiusMiles = 3958.7613;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return earthRadiusMiles * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public List<MapCluster> clusterMarkers(List<MapResult> restaurants) {
        if (restaurants == null || restaurants.isEmpty()) return List.of();
        Map<String, MapCluster> clusters = new HashMap<>();
        for (MapResult restaurant : restaurants) {
            String key = gridKey(restaurant.getLatitude(), restaurant.getLongitude());
            clusters.computeIfAbsent(key, ignored -> new MapCluster()).addRestaurant(restaurant);
        }
        return new ArrayList<>(clusters.values());
    }

    private String gridKey(double latitude, double longitude) {
        double roundedLat = Math.round(latitude * 20.0) / 20.0;
        double roundedLng = Math.round(longitude * 20.0) / 20.0;
        return String.format(Locale.ROOT, "%.2f:%.2f", roundedLat, roundedLng);
    }

    private boolean matchesCuisine(MapFilterParams filters, Restaurant restaurant) {
        if (filters.getCuisines() == null || filters.getCuisines().isEmpty()) return true;
        if (restaurant.getCuisine() == null) return false;
        return filters.getCuisines().stream().anyMatch(value -> value != null && value.equalsIgnoreCase(restaurant.getCuisine()));
    }

    private boolean matchesPriceTier(MapFilterParams filters, Restaurant restaurant) {
        if (filters.getPriceTiers() == null || filters.getPriceTiers().isEmpty()) return true;
        Integer tier = restaurant.getPriceTier();
        return tier != null && filters.getPriceTiers().contains(tier);
    }

    private boolean matchesOpenNow(MapFilterParams filters, Restaurant restaurant) {
        if (filters.getOpenNow() == null || !filters.getOpenNow()) return true;
        Boolean openNow = restaurant.getOpenNow();
        return openNow == null || openNow;
    }

    private double computeRankScore(Restaurant restaurant, Double personalizedBoost) {
        double rating = restaurant.getAvgRating() == null ? 0.0 : restaurant.getAvgRating().doubleValue();
        int reviewCount = restaurant.getReviewCount() == null ? 0 : restaurant.getReviewCount();
        double popularity = reviewCount <= 0 ? 0.0 : Math.min(1.5, Math.log10(reviewCount + 1) * 0.5);
        double personal = personalizedBoost == null ? 0.0 : personalizedBoost * 2.5;
        return rating + popularity + personal;
    }

    private Map<Long, Double> buildPersonalizedScores(long userId) {
        List<RankingEntry> entries = rankingDAO.findByUser(userId);
        if (entries.isEmpty()) return Map.of();

        int total = entries.size();
        Map<Long, Double> scores = new HashMap<>();
        for (RankingEntry entry : entries) {
            double normalized = total == 1 ? 1.0 : (double) (total - entry.getRankPosition()) / (double) (total - 1);
            scores.put(entry.getRestaurantId(), normalized);
        }
        return scores;
    }
}
