package com.swen90007.rideshare.persistence.mapper;

import com.swen90007.rideshare.domain.model.User;
import com.swen90007.rideshare.persistence.DBConnection;

import java.sql.*;

/**
 * UserMapper
 * 变更说明：
 * 1) 新增带 Connection 参数的重载方法：insert / findByEmail / findById
 * 2) 旧的无 Connection 方法仍然保留，并委托到新的重载，保证向后兼容
 */
public class UserMapper {

    /* ---------------------------------
     * 新增：带 Connection 的重载（推荐在 Service 中使用）
     * --------------------------------- */

    /** 插入用户（使用外部事务连接），返回生成的 userId */
    public int insert(Connection conn, User user) throws SQLException {
        // PostgreSQL：RETURNING id 直接返回主键
        final String sql = "INSERT INTO users (name, email, password, role) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getRole());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Insert user failed: no id returned.");
    }

    /** 通过 email 查询（使用外部事务连接） */
    public User findByEmail(Connection conn, String email) throws SQLException {
        final String sql = "SELECT id, name, email, password, role FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /** 通过 id 查询（使用外部事务连接） */
    public User findById(Connection conn, int id) throws SQLException {
        final String sql = "SELECT id, name, email, password, role FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /* -------------------------------------------------
     * 兼容保留：无 Connection 的旧方法（内部自行开关连接）
     * ------------------------------------------------- */

    /** 兼容：插入用户（内部获取连接，适合独立调用；事务外使用） */
    public int insert(User user) throws SQLException {
        Connection conn = null;
        boolean prevAutoCommit = true;
        try {
            conn = DBConnection.getConnection();
            prevAutoCommit = conn.getAutoCommit();
            if (prevAutoCommit) conn.setAutoCommit(false);

            int newId = insert(conn, user);

            conn.commit();
            return newId;
        } catch (SQLException e) {
            if (conn != null) try { conn.rollback(); } catch (SQLException ignore) {}
            throw e;
        } finally {
            if (conn != null) try { conn.setAutoCommit(prevAutoCommit); } catch (SQLException ignore) {}
            DBConnection.closeThreadConnection();
        }
    }

    /** 兼容：通过 email 查询（内部获取连接） */
    public User findByEmail(String email) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            return findByEmail(conn, email);
        } finally {
            DBConnection.closeThreadConnection();
        }
    }

    /** 兼容：通过 id 查询（内部获取连接） */
    public User findById(int id) throws SQLException {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            return findById(conn, id);
        } finally {
            DBConnection.closeThreadConnection();
        }
    }

    /* ------------------
     * 工具：行映射
     * ------------------ */
    private static User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        return u;
    }
}
