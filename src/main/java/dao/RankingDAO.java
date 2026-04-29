package dao;

import db.DBConnectionManager;
import model.RankingEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RankingDAO {

    private RankingEntry mapRanking(ResultSet rs) throws SQLException {
        RankingEntry entry = new RankingEntry();
        entry.setRankingId(rs.getLong("ranking_id"));
        entry.setUserId(rs.getLong("user_id"));
        entry.setRestaurantId(rs.getLong("restaurant_id"));
        entry.setRankPosition(rs.getInt("rank_position"));
        entry.setCreatedAt(rs.getTimestamp("created_at"));
        entry.setUpdatedAt(rs.getTimestamp("updated_at"));

        try {
            entry.setRestaurantName(rs.getString("restaurant_name"));
            entry.setRestaurantAddress(rs.getString("restaurant_address"));
            entry.setCuisine(rs.getString("cuisine"));
            int priceTier = rs.getInt("price_tier");
            entry.setPriceTier(rs.wasNull() ? null : priceTier);
        } catch (SQLException ignored) {
        }

        return entry;
    }

    public List<RankingEntry> findByUser(long userId) {
        String sql = """
                SELECT rk.ranking_id,
                       rk.user_id,
                       rk.restaurant_id,
                       rk.rank_position,
                       rk.created_at,
                       rk.updated_at,
                       r.name AS restaurant_name,
                       r.address AS restaurant_address,
                       r.cuisine_type AS cuisine,
                       r.price_tier
                FROM rankings rk
                LEFT JOIN restaurants r ON r.restaurant_id = rk.restaurant_id
                WHERE rk.user_id = ?
                ORDER BY rk.rank_position
                """;

        List<RankingEntry> rankings = new ArrayList<>();

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rankings.add(mapRanking(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rankings;
    }

    public RankingEntry findByUserAndRestaurant(long userId, long restaurantId) {
        String sql = """
                SELECT ranking_id, user_id, restaurant_id, rank_position, created_at, updated_at
                FROM rankings
                WHERE user_id = ? AND restaurant_id = ?
                """;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, restaurantId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRanking(rs);
                }
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

    private int getMaxRankPosition(Connection conn, long userId) throws SQLException {
        String sql = "SELECT COALESCE(MAX(rank_position), 0) AS max_pos FROM rankings WHERE user_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_pos");
                }
            }
        }

        return 0;
    }

    public RankingEntry createRanking(RankingEntry entry) {
        String shiftSql = """
                UPDATE rankings
                SET rank_position = rank_position + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND rank_position >= ?
                """;

        String insertSql = """
                INSERT INTO rankings (user_id, restaurant_id, rank_position)
                VALUES (?, ?, ?)
                RETURNING ranking_id, created_at, updated_at
                """;

        Connection conn = null;

        try {
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false);

            int maxPos = getMaxRankPosition(conn, entry.getUserId());
            int targetPos = entry.getRankPosition();

            if (targetPos < 1) {
                targetPos = 1;
            }
            if (targetPos > maxPos + 1) {
                targetPos = maxPos + 1;
            }

            try (PreparedStatement shiftStmt = conn.prepareStatement(shiftSql)) {
                shiftStmt.setLong(1, entry.getUserId());
                shiftStmt.setInt(2, targetPos);
                shiftStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setLong(1, entry.getUserId());
                insertStmt.setLong(2, entry.getRestaurantId());
                insertStmt.setInt(3, targetPos);

                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        entry.setRankingId(rs.getLong("ranking_id"));
                        entry.setRankPosition(targetPos);
                        entry.setCreatedAt(rs.getTimestamp("created_at"));
                        entry.setUpdatedAt(rs.getTimestamp("updated_at"));
                    }
                }
            }

            conn.commit();
            return entry;

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            return null;
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

    public boolean updateRankingPosition(long userId, long restaurantId, int newPosition) {
        final int TEMP_OFFSET = 10000;

        String findSql = """
                SELECT rank_position
                FROM rankings
                WHERE user_id = ? AND restaurant_id = ?
                """;

        String countSql = """
                SELECT COUNT(*)
                FROM rankings
                WHERE user_id = ?
                """;

        String moveCurrentToTempSql = """
                UPDATE rankings
                SET rank_position = -1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND restaurant_id = ?
                """;

        String moveRangeToTempSql = """
                UPDATE rankings
                SET rank_position = rank_position + ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND rank_position BETWEEN ? AND ?
                """;

        String shiftRangeDownSql = """
                UPDATE rankings
                SET rank_position = rank_position - ? + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND rank_position BETWEEN ? AND ?
                """;

        String shiftRangeUpSql = """
                UPDATE rankings
                SET rank_position = rank_position - ? - 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND rank_position BETWEEN ? AND ?
                """;

        String moveCurrentToFinalSql = """
                UPDATE rankings
                SET rank_position = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND restaurant_id = ?
                """;

        try (Connection conn = DBConnectionManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int oldPosition;
                int count;

                try (PreparedStatement stmt = conn.prepareStatement(findSql)) {
                    stmt.setLong(1, userId);
                    stmt.setLong(2, restaurantId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return false;
                        }
                        oldPosition = rs.getInt("rank_position");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(countSql)) {
                    stmt.setLong(1, userId);

                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        count = rs.getInt(1);
                    }
                }

                if (newPosition < 1) {
                    newPosition = 1;
                }
                if (newPosition > count) {
                    newPosition = count;
                }

                if (oldPosition == newPosition) {
                    conn.commit();
                    return true;
                }

                try (PreparedStatement stmt = conn.prepareStatement(moveCurrentToTempSql)) {
                    stmt.setLong(1, userId);
                    stmt.setLong(2, restaurantId);
                    stmt.executeUpdate();
                }

                if (newPosition < oldPosition) {
                    try (PreparedStatement stmt = conn.prepareStatement(moveRangeToTempSql)) {
                        stmt.setInt(1, TEMP_OFFSET);
                        stmt.setLong(2, userId);
                        stmt.setInt(3, newPosition);
                        stmt.setInt(4, oldPosition - 1);
                        stmt.executeUpdate();
                    }

                    try (PreparedStatement stmt = conn.prepareStatement(shiftRangeDownSql)) {
                        stmt.setInt(1, TEMP_OFFSET);
                        stmt.setLong(2, userId);
                        stmt.setInt(3, newPosition + TEMP_OFFSET);
                        stmt.setInt(4, oldPosition - 1 + TEMP_OFFSET);
                        stmt.executeUpdate();
                    }
                } else {
                    try (PreparedStatement stmt = conn.prepareStatement(moveRangeToTempSql)) {
                        stmt.setInt(1, TEMP_OFFSET);
                        stmt.setLong(2, userId);
                        stmt.setInt(3, oldPosition + 1);
                        stmt.setInt(4, newPosition);
                        stmt.executeUpdate();
                    }

                    try (PreparedStatement stmt = conn.prepareStatement(shiftRangeUpSql)) {
                        stmt.setInt(1, TEMP_OFFSET);
                        stmt.setLong(2, userId);
                        stmt.setInt(3, oldPosition + 1 + TEMP_OFFSET);
                        stmt.setInt(4, newPosition + TEMP_OFFSET);
                        stmt.executeUpdate();
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(moveCurrentToFinalSql)) {
                    stmt.setInt(1, newPosition);
                    stmt.setLong(2, userId);
                    stmt.setLong(3, restaurantId);
                    stmt.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
                return false;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteRanking(long userId, long restaurantId) {
        String findSql = "SELECT rank_position FROM rankings WHERE user_id = ? AND restaurant_id = ?";
        String deleteSql = "DELETE FROM rankings WHERE user_id = ? AND restaurant_id = ?";
        String compressSql = """
                UPDATE rankings
                SET rank_position = rank_position - 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND rank_position > ?
                """;

        Connection conn = null;

        try {
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false);

            int oldPosition;

            try (PreparedStatement findStmt = conn.prepareStatement(findSql)) {
                findStmt.setLong(1, userId);
                findStmt.setLong(2, restaurantId);

                try (ResultSet rs = findStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return false;
                    }
                    oldPosition = rs.getInt("rank_position");
                }
            }

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setLong(1, userId);
                deleteStmt.setLong(2, restaurantId);
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement compressStmt = conn.prepareStatement(compressSql)) {
                compressStmt.setLong(1, userId);
                compressStmt.setInt(2, oldPosition);
                compressStmt.executeUpdate();
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