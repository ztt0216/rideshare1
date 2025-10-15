package com.swen90007.rideshare.persistence.mapper;

import java.sql.*;

/**
 * Wallet 数据访问：仅使用 "balance"（元）字段。
 */
public class WalletMapper {

    /**
     * 确保 wallet 表对该 userId 至少有一行，若不存在则创建（余额 0）。
     */
    public void insertIfAbsent(Connection conn, int userId) throws SQLException {
        final String sql = """
            INSERT INTO wallet (user_id, balance)
            VALUES (?, 0.00)
            ON CONFLICT (user_id) DO NOTHING
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /**
     * 读取钱包余额（元，DECIMAL/NUMERIC）。
     */
    public double getBalance(Connection conn, int userId) throws SQLException {
        final String sql = "SELECT balance FROM wallet WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1).doubleValue();
                }
                throw new SQLException("Wallet not found for userId=" + userId);
            }
        }
    }

    /**
     * 乐观锁更新余额：只有当前余额仍为 expectedBalance 时才更新为 newBalance。
     * 返回 1 表示成功；0 表示失败（并发冲突）。
     */
    public int updateBalanceConditional(Connection conn,
                                        int userId,
                                        double expectedBalance,
                                        double newBalance) throws SQLException {
        final String sql = """
            UPDATE wallet
               SET balance = ?, updated_at = now()
             WHERE user_id = ? AND balance = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, java.math.BigDecimal.valueOf(newBalance));
            ps.setInt(2, userId);
            ps.setBigDecimal(3, java.math.BigDecimal.valueOf(expectedBalance));
            return ps.executeUpdate();
        }
    }
}
