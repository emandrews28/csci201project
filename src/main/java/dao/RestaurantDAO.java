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
        return restaurant;
    }

    public List<Restaurant> findNearby(double centerLat, double centerLng, double radiusMiles) {
        double latDelta = radiusMiles / 69.0;
        double lngDelta = radiusMiles / (Math.max(0.1, 69.172 * Math.cos(Math.toRadians(centerLat))));
        return findWithinBounds(centerLat + latDelta, centerLat - latDelta, centerLng + lngDelta, centerLng - lngDelta);
    }

    public List<Restaurant> findWithinBounds(double north, double south, double east, double west) {
        String sql = """
                SELECT restaurant_id, name, address, latitude, longitude, price_tier, cuisine,
                       is_open_now, avg_rating, review_count, created_at, updated_at
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
                SELECT restaurant_id, name, address, latitude, longitude, price_tier, cuisine,
                       is_open_now, avg_rating, review_count, created_at, updated_at
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
}
