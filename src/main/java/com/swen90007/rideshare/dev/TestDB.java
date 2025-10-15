package com.swen90007.rideshare.dev;

import com.swen90007.rideshare.persistence.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * 简单的数据库连通性测试。
 * 默认使用 DBConnection 中“固定写死”的 Render PostgreSQL 连接。
 */
public class TestDB {
    public static void main(String[] args) {
        System.out.println("[TestDB] trying to connect…");
        try (Connection conn = DBConnection.openConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 1) {
                    System.out.println("[TestDB] OK: SELECT 1 returned 1 (connection successful).");
                } else {
                    System.out.println("[TestDB] Unexpected result from SELECT 1.");
                }
            }

            // 额外：尝试探测几张核心表是否存在
            String[] tables = {"users", "wallet", "ride", "payment", "availability"};
            for (String t : tables) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT to_regclass(?);")) {
                    ps.setString(1, t);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        String reg = rs.getString(1);
                        System.out.println("[TestDB] table '" + t + "': " +
                                (reg != null ? "FOUND" : "NOT FOUND"));
                    }
                } catch (Exception ignored) {
                    System.out.println("[TestDB] table '" + t + "': check failed");
                }
            }
        } catch (Exception e) {
            System.err.println("[TestDB] FAILED to connect:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
