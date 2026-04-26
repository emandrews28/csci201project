package dao;

import db.DBConnectionManager;
import model.Photo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class PhotoDAO {
	
    private static final String PHOTO_SELECT = """
            SELECT p.photo_id,
                   p.user_id,
                   p.restaurant_id,
                   p.review_id,
                   p.image_url,
                   p.caption,
                   p.created_at,
                   r.name AS restaurant_name,
                   u.username AS username
            FROM photos p
            LEFT JOIN restaurants r ON r.restaurant_id = p.restaurant_id
            LEFT JOIN users u ON u.user_id = p.user_id
            """;

    private Photo mapPhoto(ResultSet rs) throws SQLException {
        Photo photo = new Photo();
        photo.setPhotoId(rs.getLong("photo_id"));
        photo.setUserId(rs.getLong("user_id"));
        photo.setRestaurantId(rs.getLong("restaurant_id"));
        long reviewId = rs.getLong("review_id");
        photo.setReviewId(rs.wasNull() ? null : reviewId);
        photo.setImageUrl(rs.getString("image_url"));
        photo.setCaption(rs.getString("caption"));
        photo.setCreatedAt(rs.getTimestamp("created_at"));

        try {
            photo.setRestaurantName(rs.getString("restaurant_name"));
            photo.setUsername(rs.getString("username"));
        } catch (SQLException ignored) {
        }

        return photo;
    }

    public Photo createPhoto(Photo photo) {
        String sql = """
                INSERT INTO photos (user_id, restaurant_id, review_id, image_url, caption)
                VALUES (?, ?, ?, ?, ?)
                RETURNING photo_id, created_at
                """;
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, photo.getUserId());
            stmt.setLong(2, photo.getRestaurantId());
            if (photo.getReviewId() == null) {
                stmt.setNull(3, Types.BIGINT);
            } else {
                stmt.setLong(3, photo.getReviewId());
            }
            stmt.setString(4, photo.getImageUrl());
            stmt.setString(5, photo.getCaption());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    photo.setPhotoId(rs.getLong("photo_id"));
                    photo.setCreatedAt(rs.getTimestamp("created_at"));
                    return photo;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Photo findById(long photoId) {
        String sql = PHOTO_SELECT + """
                WHERE p.photo_id = ?
                """;
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, photoId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapPhoto(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Photo> findByRestaurant(long restaurantId) {
        String sql = PHOTO_SELECT + """
                WHERE p.restaurant_id = ?
                ORDER BY p.created_at DESC
                """;
        return queryList(sql, restaurantId);
    }

    public List<Photo> findByUser(long userId) {
        String sql = PHOTO_SELECT + """
                WHERE p.user_id = ?
                ORDER BY p.created_at DESC
                """;
        return queryList(sql, userId);
    }

    public List<Photo> findByReview(long reviewId) {
        String sql = PHOTO_SELECT + """
                WHERE p.review_id = ?
                ORDER BY p.created_at ASC
                """;
        return queryList(sql, reviewId);
    }

    private List<Photo> queryList(String sql, long id) {
        List<Photo> photos = new ArrayList<>();
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) photos.add(mapPhoto(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return photos;
    }

    public boolean updateCaption(long photoId, long userId, String caption) {
        String sql = "UPDATE photos SET caption = ? WHERE photo_id = ? AND user_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, caption);
            stmt.setLong(2, photoId);
            stmt.setLong(3, userId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePhoto(long photoId, long userId) {
        String sql = "DELETE FROM photos WHERE photo_id = ? AND user_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, photoId);
            stmt.setLong(2, userId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean restaurantExists(long restaurantId) {
        String sql = "SELECT 1 FROM restaurants WHERE restaurant_id = ?";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, restaurantId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
