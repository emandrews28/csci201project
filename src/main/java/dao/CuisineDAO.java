package dao;

import db.DBConnectionManager;
import model.Cuisine;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CuisineDAO {

    /** Returns all cuisines ordered by parent first, then name — good for building a grouped dropdown. */
    public List<Cuisine> findAll() {
        List<Cuisine> list = new ArrayList<>();
        String sql = "SELECT cuisine_id, name, slug, parent_id FROM cuisines ORDER BY parent_id NULLS FIRST, name ASC";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Cuisine c = new Cuisine();
                c.setCuisineId(rs.getLong("cuisine_id"));
                c.setName(rs.getString("name"));
                c.setSlug(rs.getString("slug"));
                long parentId = rs.getLong("parent_id");
                c.setParentId(rs.wasNull() ? null : parentId);
                list.add(c);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching cuisines", e);
        }
        return list;
    }

    /**
     * Returns all cuisine IDs in the subtree rooted at cuisineId (inclusive).
     * Used by SearchFilterManager to expand a parent cuisine selection to all children.
     */
    public List<Long> expandCuisineTree(long cuisineId) {
        List<Long> ids = new ArrayList<>();
        String sql = "WITH RECURSIVE tree AS (" +
                     "  SELECT cuisine_id FROM cuisines WHERE cuisine_id = ? " +
                     "  UNION ALL " +
                     "  SELECT c.cuisine_id FROM cuisines c JOIN tree t ON c.parent_id = t.cuisine_id" +
                     ") SELECT cuisine_id FROM tree";
        try (Connection conn = DBConnectionManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cuisineId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getLong("cuisine_id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error expanding cuisine tree for id " + cuisineId, e);
        }
        return ids;
    }
}
