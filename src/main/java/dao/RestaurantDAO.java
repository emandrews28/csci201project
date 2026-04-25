package dao;

import db.DBConnectionManager;
import model.Restaurant;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import model.FilterParams;

public class RestaurantDAO {

    private Restaurant mapRestaurant(ResultSet rs) throws SQLException {
        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantId(rs.getLong("restaurant_id"));
        restaurant.setName(rs.getString("name"));
        restaurant.setAddress(rs.getString("address"));

        double latitude = rs.getDouble("latitude");
        restaurant.setLatitude(rs.wasNull() ? null : latitude);

        double longitude = rs.getDouble("longitude");
        restaurant.setLongitude(rs.wasNull() ? null : longitude);

        int priceTier = rs.getInt("price_tier");
        restaurant.setPriceTier(rs.wasNull() ? null : priceTier);

        restaurant.setCuisine(rs.getString("cuisine"));

        boolean openNow = rs.getBoolean("is_open_now");
        restaurant.setOpenNow(rs.wasNull() ? null : openNow);

        BigDecimal avgRating = rs.getBigDecimal("avg_rating");
        restaurant.setAvgRating(avgRating);

        int reviewCount = rs.getInt("review_count");
        restaurant.setReviewCount(rs.wasNull() ? null : reviewCount);

        restaurant.setCreatedAt(rs.getTimestamp("created_at"));
        restaurant.setUpdatedAt(rs.getTimestamp("updated_at"));

        restaurant.setGooglePlaceId(rs.getString("google_place_id"));
        
        return restaurant;
    }

    public List<Restaurant> findNearby(double centerLat, double centerLng, double radiusMiles) {
        double latDelta = radiusMiles / 69.0;
        double lngDelta = radiusMiles / (Math.max(0.1, 69.172 * Math.cos(Math.toRadians(centerLat))));
        return findWithinBounds(centerLat + latDelta, centerLat - latDelta, centerLng + lngDelta, centerLng - lngDelta);
    }

    public List<Restaurant> findWithinBounds(double north, double south, double east, double west) {
        String sql = """
                SELECT restaurant_id, name, address, latitude, longitude, price_tier, cuisine_type AS cuisine,
                       is_open_now, avg_rating, review_count, created_at, updated_at, google_place_id
                FROM restaurants
                WHERE latitude IS NOT NULL
                  AND longitude IS NOT NULL
                  AND latitude BETWEEN ? AND ?
                  AND longitude BETWEEN ? AND ?
                ORDER BY restaurant_id
                """;

        List<Restaurant> restaurants = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, south);
            stmt.setDouble(2, north);
            stmt.setDouble(3, west);
            stmt.setDouble(4, east);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) restaurants.add(mapRestaurant(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return restaurants;
    }

    public List<Restaurant> findByIds(List<Long> restaurantIds) {
        if (restaurantIds == null || restaurantIds.isEmpty()) return List.of();

        StringBuilder sql = new StringBuilder("""
                SELECT restaurant_id, name, address, latitude, longitude, price_tier, cuisine_type AS cuisine,
                       is_open_now, avg_rating, review_count, created_at, updated_at, google_place_id
                FROM restaurants
                WHERE restaurant_id IN (
                """);
        for (int i = 0; i < restaurantIds.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(") ORDER BY restaurant_id");

        List<Restaurant> restaurants = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < restaurantIds.size(); i++) stmt.setLong(i + 1, restaurantIds.get(i));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) restaurants.add(mapRestaurant(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return restaurants;
    }

    // for search & filtering
    public List<Restaurant> search(FilterParams params, long userId) {
        List<Restaurant> results = new ArrayList<>();
        List<Object> paramValues = new ArrayList<>();
        String sql = buildSearchQuery(params, userId, paramValues);

        try (Connection conn = DBConnectionManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < paramValues.size(); i++) {
                Object val = paramValues.get(i);
                if (val instanceof Long)         ps.setLong(i + 1, (Long) val);
                else if (val instanceof Integer) ps.setInt(i + 1, (Integer) val);
                else if (val instanceof Double)  ps.setDouble(i + 1, (Double) val);
                else if (val instanceof String)  ps.setString(i + 1, (String) val);
                else ps.setObject(i + 1, val);
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Restaurant r = mapRestaurant(rs);
                if (params.hasLocation()) r.setDistanceMiles(rs.getDouble("distance_miles"));
                r.setCuisines(findCuisinesForRestaurant(r.getRestaurantId()));
                results.add(r);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    private String buildSearchQuery(FilterParams params, long userId, List<Object> paramValues) {
        StringBuilder sql = new StringBuilder();

        sql.append("SELECT DISTINCT r.restaurant_id, r.name, r.address, r.latitude, r.longitude, ")
        .append("r.price_tier, r.cuisine_type AS cuisine, r.is_open_now, r.avg_rating, r.review_count, ")
        .append("r.created_at, r.updated_at, r.google_place_id");

        if (params.hasLocation()) {
            sql.append(", (3958.8 * acos(LEAST(1.0, ")
            .append("cos(radians(?)) * cos(radians(r.latitude)) * cos(radians(r.longitude) - radians(?)) + ")
            .append("sin(radians(?)) * sin(radians(r.latitude))))) AS distance_miles");
            paramValues.add(params.getUserLat());
            paramValues.add(params.getUserLng());
            paramValues.add(params.getUserLat());
        }

        sql.append(" FROM restaurants r");

        if (params.getCuisineIds() != null && !params.getCuisineIds().isEmpty()) {
            sql.append(" JOIN restaurant_cuisines rc ON rc.restaurant_id = r.restaurant_id");
        }

        if (params.isFriendsOnly()) {
            sql.append(" JOIN reviews rv ON rv.restaurant_id = r.restaurant_id")
            .append(" JOIN follows f ON f.following_id = rv.user_id AND f.follower_id = ?");
            paramValues.add(userId);
        }

        sql.append(" WHERE 1=1");

        if (params.getQuery() != null && !params.getQuery().isBlank()) {
            sql.append(" AND r.name ILIKE ?");
            paramValues.add("%" + params.getQuery().trim() + "%");
        }

        if (params.getCuisineIds() != null && !params.getCuisineIds().isEmpty()) {
            sql.append(" AND rc.cuisine_id IN (");
            for (int i = 0; i < params.getCuisineIds().size(); i++) {
                sql.append(i == 0 ? "?" : ",?");
                paramValues.add(params.getCuisineIds().get(i));
            }
            sql.append(")");
        }

        if (params.getPriceTiers() != null && !params.getPriceTiers().isEmpty()) {
            sql.append(" AND r.price_tier IN (");
            for (int i = 0; i < params.getPriceTiers().size(); i++) {
                sql.append(i == 0 ? "?" : ",?");
                paramValues.add(params.getPriceTiers().get(i));
            }
            sql.append(")");
        }

        if (params.hasDistanceFilter()) {
            String inner = sql.toString();
            StringBuilder outer = new StringBuilder();
            outer.append("SELECT * FROM (").append(inner).append(") AS filtered WHERE distance_miles <= ?");
            paramValues.add(params.getRadiusMiles());
            appendOrderAndPagination(outer, params, paramValues, true);
            return outer.toString();
        }

        appendOrderAndPagination(sql, params, paramValues, false);
        return sql.toString();
    }

    private void appendOrderAndPagination(StringBuilder sql, FilterParams params,
                                        List<Object> paramValues, boolean hasDistanceAlias) {
        switch (params.getSortBy()) {
            case "distance":
                sql.append(hasDistanceAlias || params.hasLocation()
                    ? " ORDER BY distance_miles ASC NULLS LAST"
                    : " ORDER BY r.name ASC");
                break;
            case "newest":
                sql.append(" ORDER BY r.created_at DESC");
                break;
            case "top_rated":
            default:
                sql.append(" ORDER BY (COALESCE(r.avg_rating, 0) * 0.5 + LOG(COALESCE(r.review_count, 0) + 1) * 0.3) DESC");
                break;
        }
        sql.append(" LIMIT ? OFFSET ?");
        paramValues.add(params.getLimit());
        paramValues.add(params.getPage() * params.getLimit());
    }

    public List<model.Cuisine> findCuisinesForRestaurant(long restaurantId) {
        List<model.Cuisine> list = new ArrayList<>();
        String sql = "SELECT c.cuisine_id, c.name, c.slug, c.parent_id " +
                    "FROM cuisines c JOIN restaurant_cuisines rc ON rc.cuisine_id = c.cuisine_id " +
                    "WHERE rc.restaurant_id = ? ORDER BY rc.is_primary DESC";
        try (Connection conn = DBConnectionManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, restaurantId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.Cuisine c = new model.Cuisine();
                c.setCuisineId(rs.getLong("cuisine_id"));
                c.setName(rs.getString("name"));
                c.setSlug(rs.getString("slug"));
                long parentId = rs.getLong("parent_id");
                c.setParentId(rs.wasNull() ? null : parentId);
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public Restaurant findById(long restaurantId) {
        String sql = "SELECT restaurant_id, name, address, latitude, longitude, price_tier, cuisine_type AS cuisine, " +
                    "is_open_now, avg_rating, review_count, created_at, updated_at, google_place_id " +
                    "FROM restaurants WHERE restaurant_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, restaurantId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Restaurant r = mapRestaurant(rs);
                r.setCuisines(findCuisinesForRestaurant(restaurantId));
                return r;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public List<Restaurant> searchByName(String query, int limit) {
        String sql = """
                SELECT restaurant_id, name, address, cuisine_type AS cuisine, price_tier
                FROM restaurants
                WHERE name ILIKE ?
                ORDER BY name
                LIMIT ?
                """;

        List<Restaurant> restaurants = new ArrayList<>();

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + query.trim() + "%");
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Restaurant restaurant = new Restaurant();

                    restaurant.setRestaurantId(rs.getLong("restaurant_id"));
                    restaurant.setName(rs.getString("name"));
                    restaurant.setAddress(rs.getString("address"));
                    restaurant.setCuisine(rs.getString("cuisine"));

                    int priceTier = rs.getInt("price_tier");
                    restaurant.setPriceTier(rs.wasNull() ? null : priceTier);

                    restaurants.add(restaurant);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return restaurants;
    }
}