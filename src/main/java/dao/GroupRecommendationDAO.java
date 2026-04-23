package dao;

import db.DBConnectionManager;
import model.RankingEntry;
import model.RecommendationGroup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class GroupRecommendationDAO {

    public RecommendationGroup createGroup(RecommendationGroup group) {
        String sql = "INSERT INTO recommendation_groups (created_by, group_name) VALUES (?, ?) RETURNING group_id, created_at";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, group.getCreatedBy());
            ps.setString(2, group.getGroupName());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    group.setGroupId(rs.getLong("group_id"));
                    group.setCreatedAt(rs.getTimestamp("created_at"));
                    return group;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean addGroupMember(long groupId, long userId) {
        String sql = "INSERT INTO recommendation_group_members (group_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            ps.setLong(2, userId);
            int n = ps.executeUpdate();
            return n > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeGroupMember(long groupId, long userId) {
        // enforce minimum group size of 2 users
        String countSql = "SELECT COUNT(*) AS cnt FROM recommendation_group_members WHERE group_id = ?";
        String existsSql = "SELECT 1 FROM recommendation_group_members WHERE group_id = ? AND user_id = ?";
        String deleteSql = "DELETE FROM recommendation_group_members WHERE group_id = ? AND user_id = ?";

        try (Connection conn = DBConnectionManager.getConnection()) {
            // check membership exists
            try (PreparedStatement existsPs = conn.prepareStatement(existsSql)) {
                existsPs.setLong(1, groupId);
                existsPs.setLong(2, userId);
                try (ResultSet rs = existsPs.executeQuery()) {
                    if (!rs.next()) return false; // not a member
                }
            }

            int currentCount = 0;
            try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                countPs.setLong(1, groupId);
                try (ResultSet rs = countPs.executeQuery()) {
                    if (rs.next()) currentCount = rs.getInt("cnt");
                }
            }

            if (currentCount <= 2) {
                // cannot remove because group must have at least 2 users
                return false;
            }

            try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                deletePs.setLong(1, groupId);
                deletePs.setLong(2, userId);
                int n = deletePs.executeUpdate();
                return n > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Long> getGroupMembers(long groupId) {
        List<Long> list = new ArrayList<>();
        String sql = "SELECT user_id FROM recommendation_group_members WHERE group_id = ? ORDER BY joined_at";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getLong("user_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<RankingEntry> getRankingsForUser(long userId) {
        RankingDAO rankingDAO = new RankingDAO();
        return rankingDAO.findByUser(userId);
    }

}