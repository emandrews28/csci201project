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
        return entry;
    }

    public List<RankingEntry> findByUser(long userId) {
        String sql = """
                SELECT ranking_id, user_id, restaurant_id, rank_position, created_at, updated_at
                FROM rankings
                WHERE user_id = ?
                ORDER BY rank_position
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
        String findSql = "SELECT rank_position FROM rankings WHERE user_id = ? AND restaurant_id = ?";
        String moveTargetAwaySql = """
                UPDATE rankings
                SET rank_position = ?, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND restaurant_id = ?
                """;
        String moveUpSql = """
                UPDATE rankings
                SET rank_position = rank_position + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND rank_position >= ? AND rank_position < ?
                """;
        String moveDownSql = """
                UPDATE rankings
                SET rank_position = rank_position - 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND rank_position <= ? AND rank_position > ?
                """;
        String moveTargetToFinalSql = """
                UPDATE rankings
                SET rank_position = ?, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND restaurant_id = ?
                """;

        Connection conn = null;

        try {
            conn = DBConnectionManager.getConnection();
            conn.setAutoCommit(false);

            int oldPosition;

            // Find current position of the target ranking
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

            int maxPos = getMaxRankPosition(conn, userId);

            if (newPosition < 1) {
                newPosition = 1;
            }
            if (newPosition > maxPos) {
                newPosition = maxPos;
            }

            if (newPosition == oldPosition) {
                conn.rollback();
                return true;
            }

            // Move target row to a temporary safe position first
            int tempPosition = maxPos + 1;

            try (PreparedStatement tempStmt = conn.prepareStatement(moveTargetAwaySql)) {
                tempStmt.setInt(1, tempPosition);
                tempStmt.setLong(2, userId);
                tempStmt.setLong(3, restaurantId);
                tempStmt.executeUpdate();
            }

            // Shift other rows
            if (newPosition < oldPosition) {
                // Example: move from 5 -> 2, shift 2..4 up by 1
                try (PreparedStatement moveUpStmt = conn.prepareStatement(moveUpSql)) {
                    moveUpStmt.setLong(1, userId);
                    moveUpStmt.setInt(2, newPosition);
                    moveUpStmt.setInt(3, oldPosition);
                    moveUpStmt.executeUpdate();
                }
            } else {
                // Example: move from 2 -> 5, shift 3..5 down by 1
                try (PreparedStatement moveDownStmt = conn.prepareStatement(moveDownSql)) {
                    moveDownStmt.setLong(1, userId);
                    moveDownStmt.setInt(2, newPosition);
                    moveDownStmt.setInt(3, oldPosition);
                    moveDownStmt.executeUpdate();
                }
            }

            // Put target row into final position
            try (PreparedStatement finalStmt = conn.prepareStatement(moveTargetToFinalSql)) {
                finalStmt.setInt(1, newPosition);
                finalStmt.setLong(2, userId);
                finalStmt.setLong(3, restaurantId);
                finalStmt.executeUpdate();
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