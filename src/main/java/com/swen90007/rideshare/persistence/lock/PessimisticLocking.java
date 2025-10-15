package com.swen90007.rideshare.persistence.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Row-level pessimistic lock helpers. */
public class PessimisticLocking {

    /** Lock ride row to serialize state transitions (avoid cancel/accept/begin/complete race). */
    public static void lockRide(Connection conn, int rideId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT version FROM ride WHERE id=? FOR UPDATE")) {
            ps.setInt(1, rideId); // 这里 setInt OK，因为 rideId 范围可控
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Ride " + rideId + " not found for lock.");
                }
                // ❌ 不要：Integer ver = (Integer) rs.getObject("version");
                // ✅ 要么直接 int，要么先 long 再收窄：
                int version = rs.getInt("version"); // 版本列是 integer，就用 getInt
                // long v = rs.getLong("version");
                // int version = Math.toIntExact(v);
            }
        }
    }
}
