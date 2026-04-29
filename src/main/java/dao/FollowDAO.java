package dao;

import db.DBConnectionManager;
import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FollowDAO {

    // Converts a database row into a User object 
    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        return user;
    }

    // Inserts a new row into the follows table — ON CONFLICT DO NOTHING means if they already follow, nothing breaks
    public boolean followUser(long followerId, long followingId) {
        String sql = """
                INSERT INTO follows (follower_id, following_id, created_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT DO NOTHING
                """;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, followerId);
            stmt.setLong(2, followingId);

            // executeUpdate() returns the number of rows affected — 1 means success, 0 means the row already existed
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Deletes the follow relationship between two users
    public boolean unfollowUser(long followerId, long followingId) {
        String sql = """
                DELETE FROM follows
                WHERE follower_id = ? AND following_id = ?
                """;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, followerId);
            stmt.setLong(2, followingId);

            // executeUpdate() returns 1 if a row was deleted, 0 if no match was found
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Checks whether a follow relationship exists between two users — used to show follow/unfollow button state
    public boolean isFollowing(long followerId, long followingId) {
        String sql = """
                SELECT 1 FROM follows
                WHERE follower_id = ? AND following_id = ?
                """;

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, followerId);
            stmt.setLong(2, followingId);

            try (ResultSet rs = stmt.executeQuery()) {
                // rs.next() returns true if at least one row was found
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Returns all users that the given user is following (their "following" list)
    public List<User> getFollowing(long userId) {
        // JOIN follows to users so we get full user info for each person being followed
        String sql = """
                SELECT u.user_id, u.username, u.email, u.created_at
                FROM users u
                JOIN follows f ON u.user_id = f.following_id
                WHERE f.follower_id = ?
                """;

        List<User> following = new ArrayList<>();

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    following.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return following;
    }

    // Returns all users that follow the given user (their "followers" list)
    public List<User> getFollowers(long userId) {
        // JOIN follows to users so we get full user info for each follower
        String sql = """
                SELECT u.user_id, u.username, u.email, u.created_at
                FROM users u
                JOIN follows f ON u.user_id = f.follower_id
                WHERE f.following_id = ?
                """;

        List<User> followers = new ArrayList<>();

        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    followers.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return followers;
    }
}
