package dao;

import db.DBConnectionManager;
import model.Review;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    private Review mapReview(ResultSet rs) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getLong("review_id"));
        review.setUserId(rs.getLong("user_id"));
        review.setRestaurantId(rs.getLong("restaurant_id"));
        review.setRankingScore(rs.getInt("ranking_score"));
        review.setReviewText(rs.getString("review_text"));
        review.setTimestamp(rs.getTimestamp("timestamp"));
        return review;
    }

    public Review findByUserAndRestaurant(long userId, long restaurantId) {
        String sql = """
                SELECT review_id, user_id, restaurant_id, ranking_score, review_text, timestamp
                FROM reviews
                WHERE user_id = ? AND restaurant_id = ?
                """;
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapReview(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Review> findByUser(long userId) {
        String sql = """
                SELECT review_id, user_id, restaurant_id, ranking_score, review_text, timestamp
                FROM reviews
                WHERE user_id = ?
                """;
        List<Review> reviews = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) reviews.add(mapReview(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return reviews;
    }

    public Review createReview(Review review) {
        String sql = """
                INSERT INTO reviews (user_id, restaurant_id, ranking_score, review_text)
                VALUES (?, ?, ?, ?)
                RETURNING review_id, timestamp
                """;
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, review.getUserId());
            stmt.setLong(2, review.getRestaurantId());
            stmt.setInt(3, review.getRankingScore());
            stmt.setString(4, review.getReviewText());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    review.setReviewId(rs.getLong("review_id"));
                    review.setTimestamp(rs.getTimestamp("timestamp"));
                    updateRestaurantRating(review.getRestaurantId());
                    return review;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateReview(Review review) {
        String sql = """
                UPDATE reviews
                SET ranking_score = ?, review_text = ?, timestamp = CURRENT_TIMESTAMP
                WHERE user_id = ? AND restaurant_id = ?
                """;
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, review.getRankingScore());
            stmt.setString(2, review.getReviewText());
            stmt.setLong(3, review.getUserId());
            stmt.setLong(4, review.getRestaurantId());
            boolean updated = stmt.executeUpdate() == 1;
            if (updated) updateRestaurantRating(review.getRestaurantId());
            return updated;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void updateRestaurantRating(long restaurantId) {
        String sql = """
                UPDATE restaurants
                SET avg_rating = (SELECT AVG(ranking_score) FROM reviews WHERE restaurant_id = ?),
                    review_count = (SELECT COUNT(*) FROM reviews WHERE restaurant_id = ?)
                WHERE restaurant_id = ?
                """;
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, restaurantId);
            stmt.setLong(2, restaurantId);
            stmt.setLong(3, restaurantId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}