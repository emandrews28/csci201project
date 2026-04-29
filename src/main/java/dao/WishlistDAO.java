package dao;

import db.DBConnectionManager;
import model.Wishlist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WishlistDAO {

    private Wishlist mapWishlist(ResultSet rs) throws SQLException {
        Wishlist entry = new Wishlist();
        entry.setWishlistId(rs.getLong("wishlist_id"));
        entry.setUserId(rs.getLong("user_id"));
        entry.setRestaurantId(rs.getLong("restaurant_id"));
        entry.setNotes(rs.getString("notes"));
        entry.setAddedAt(rs.getTimestamp("added_at"));
        return entry;
    }

    public List<Wishlist> findByUser(long userId) {
        String sql = """
                SELECT w.wishlist_id, w.user_id, w.restaurant_id, w.notes, w.added_at,
                       r.name AS restaurant_name, r.address, r.cuisine_type AS cuisine, r.price_tier
                FROM wishlist w
                JOIN restaurants r ON r.restaurant_id = w.restaurant_id
                WHERE w.user_id = ?
                ORDER BY w.added_at DESC
                """;

        List<Wishlist> entries = new ArrayList<>();

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Wishlist entry = mapWishlist(rs);
                    entry.setRestaurantName(rs.getString("restaurant_name"));
                    entry.setRestaurantAddress(rs.getString("address"));
                    entry.setCuisine(rs.getString("cuisine"));
                    int priceTier = rs.getInt("price_tier");
                    entry.setPriceTier(rs.wasNull() ? null : priceTier);
                    entries.add(entry);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return entries;
    }

    public Wishlist findByUserAndRestaurant(long userId, long restaurantId) {
        String sql = """
                SELECT wishlist_id, user_id, restaurant_id, notes, added_at
                FROM wishlist
                WHERE user_id = ? AND restaurant_id = ?
                """;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, restaurantId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapWishlist(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
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

    public Wishlist createWishlist(Wishlist entry) {
        String sql = """
                INSERT INTO wishlist (user_id, restaurant_id, notes)
                VALUES (?, ?, ?)
                RETURNING wishlist_id, added_at
                """;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, entry.getUserId());
            stmt.setLong(2, entry.getRestaurantId());
            stmt.setString(3, entry.getNotes());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    entry.setWishlistId(rs.getLong("wishlist_id"));
                    entry.setAddedAt(rs.getTimestamp("added_at"));
                    return entry;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean updateNotes(long userId, long restaurantId, String notes) {
        String sql = "UPDATE wishlist SET notes = ? WHERE user_id = ? AND restaurant_id = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, notes);
            stmt.setLong(2, userId);
            stmt.setLong(3, restaurantId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteWishlist(long userId, long restaurantId) {
        String sql = "DELETE FROM wishlist WHERE user_id = ? AND restaurant_id = ?";

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, restaurantId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Move a wishlist entry into the user's visited list (rankings table).
     * Appends to the end of the ranking list and removes the wishlist entry.
     * If the restaurant is already in the ranking list, only the wishlist row is removed.
     * Runs as a single transaction.
     */
    public boolean moveToVisited(long userId, long restaurantId) {
        String findWishlistSql = "SELECT 1 FROM wishlist WHERE user_id = ? AND restaurant_id = ?";
        String findRankingSql = "SELECT 1 FROM rankings WHERE user_id = ? AND restaurant_id = ?";
        String maxPosSql = "SELECT COALESCE(MAX(rank_position), 0) AS max_pos FROM rankings WHERE user_id = ?";
        String insertRankingSql = """
                INSERT INTO rankings (user_id, restaurant_id, rank_position)
                VALUES (?, ?, ?)
                """;
        String deleteWishlistSql = "DELETE FROM wishlist WHERE user_id = ? AND restaurant_id = ?";

        Connection conn = null;

        try {
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(findWishlistSql)) {
                stmt.setLong(1, userId);
                stmt.setLong(2, restaurantId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            boolean alreadyRanked;
            try (PreparedStatement stmt = conn.prepareStatement(findRankingSql)) {
                stmt.setLong(1, userId);
                stmt.setLong(2, restaurantId);
                try (ResultSet rs = stmt.executeQuery()) {
                    alreadyRanked = rs.next();
                }
            }

            if (!alreadyRanked) {
                int nextPos;
                try (PreparedStatement stmt = conn.prepareStatement(maxPosSql)) {
                    stmt.setLong(1, userId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        nextPos = rs.getInt("max_pos") + 1;
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(insertRankingSql)) {
                    stmt.setLong(1, userId);
                    stmt.setLong(2, restaurantId);
                    stmt.setInt(3, nextPos);
                    stmt.executeUpdate();
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(deleteWishlistSql)) {
                stmt.setLong(1, userId);
                stmt.setLong(2, restaurantId);
                stmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }
}