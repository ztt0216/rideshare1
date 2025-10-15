package com.swen90007.rideshare.persistence.mapper;

import com.swen90007.rideshare.domain.model.Ride;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Ride 数据访问层：覆盖请求/接单/开始/完成/取消/查找/通用改状态等操作。
 * 表字段对齐你现在的 ride 表结构：
 * id, rider_id, driver_id, pickup_postcode, dest_postcode, fare, status,
 * created_at, accepted_at, started_at, completed_at, cancelled_at, version
 */
public class RideMapper {

    //    time of melbourne
    private static final ZoneId MEL = ZoneId.of("Australia/Melbourne");

    /**
     * Rider 请求打车：插入 REQUESTED 记录并返回生成的 rideId
     */
    public int insert(Connection conn, int riderId, String pickupPostcode,
                      String destPostcode, double fare) throws SQLException {
        final String sql = """
                  INSERT INTO ride (rider_id, pickup_postcode, dest_postcode, fare, status, created_at)
                  VALUES (?, ?, ?, ?, 'REQUESTED', ?)
                  RETURNING id
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, riderId);
            ps.setString(2, pickupPostcode);
            ps.setString(3, destPostcode);
            ps.setBigDecimal(4, java.math.BigDecimal.valueOf(fare));
            ps.setObject(5, OffsetDateTime.now(MEL));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                throw new SQLException("Insert ride failed (no id returned)");
            }
        }
    }

    /**
     * 主键查询
     */
    // RideMapper.java 中，保留类名与其它方法不变，只替换 findById 这个方法
    public Ride findById(Connection conn, int rideId) throws SQLException {
        final String sql = """
                    SELECT id,
                           rider_id,
                           driver_id,
                           pickup_postcode,
                           dest_postcode,
                           fare,
                           status,
                           created_at,
                           accepted_at,
                           started_at,
                           completed_at,
                           cancelled_at,
                           version
                      FROM ride
                     WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rideId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Ride r = new Ride();

                // --- 关键点：不要 (Integer) 强转 getObject() 的返回值 ---
                // BIGINT -> 用 getLong 读取，再安全地下转 int
                r.setId(Math.toIntExact(rs.getLong("id")));
                r.setRiderId(Math.toIntExact(rs.getLong("rider_id")));

                long driverLong = rs.getLong("driver_id");
                if (rs.wasNull()) {
                    // 数据库为 NULL -> Java 用 null 表示
                    r.setDriverId(null);
                } else {
                    r.setDriverId(Math.toIntExact(driverLong));
                }

                // 列名按你的表结构：pickup_postcode / dest_postcode
                r.setPickup(rs.getString("pickup_postcode"));
                r.setDestination(rs.getString("dest_postcode"));

                // fare 是 integer，用 getInt 即可
                r.setFare(rs.getInt("fare"));

                r.setStatus(rs.getString("status"));

                // 下面这些时间字段如果 Ride 有对应 setter 再放开即可；
                // 你的实体若是 LocalDateTime，就用 rs.getTimestamp(...).toLocalDateTime()
                // Timestamp tsCreated = rs.getTimestamp("created_at");
                // if (tsCreated != null) r.setCreatedAt(tsCreated.toLocalDateTime());
                // 同理 accepted_at / started_at / completed_at / cancelled_at

                // version：如果你的实体有该字段再设置
                // r.setVersion(rs.getInt("version"));

                return r;
            }
        }
    }


    /**
     * 司机接单：仅当当前状态为 REQUESTED 才能接单，设置 driver 与 accepted_at
     */
    public int accept(Connection conn, int rideId, int driverId) throws SQLException {
        final String sql = """
                UPDATE ride
                SET driver_id = ?, accepted_at = ?, status = 'ACCEPTED'
                WHERE id = ? AND status = 'REQUESTED'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, driverId);
            ps.setObject(2, OffsetDateTime.now(MEL));   // ★ 墨尔本时间写入 timestamptz
            ps.setInt(3, rideId);
            return ps.executeUpdate();
        }
    }

    /**
     * 开始行程：仅当状态为 ACCEPTED 时更新为 ENROUTE 并写入 started_at
     */
    public int begin(Connection conn, int rideId) throws SQLException {
        final String sql = """
                UPDATE ride
                SET started_at = ?, status = 'ENROUTE'
                WHERE id = ? AND status = 'ACCEPTED'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, OffsetDateTime.now(MEL));  // ★
            ps.setInt(2, rideId);
            return ps.executeUpdate();
        }
    }

    /**
     * 完成行程：仅当状态为 ENROUTE 时更新为 COMPLETED 并写入 completed_at
     */
    public int complete(Connection conn, int rideId) throws SQLException {
        final String sql = """
                UPDATE ride
                SET completed_at = ?, status = 'COMPLETED'
                WHERE id = ? AND status = 'ENROUTE'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, OffsetDateTime.now(MEL)); // ★ 墨尔本时间 → timestamptz
            ps.setInt(2, rideId);
            return ps.executeUpdate();
        }
    }

    /**
     * 取消行程：在 REQUESTED/ACCEPTED 阶段允许取消
     */
    public int cancel(Connection conn, int rideId) throws SQLException {
        final String sql = """
                UPDATE ride
                SET cancelled_at = ?, status = 'CANCELLED'
                WHERE id = ? AND status IN ('REQUESTED','ACCEPTED')
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, OffsetDateTime.now(MEL)); // ★
            ps.setInt(2, rideId);
            return ps.executeUpdate();
        }
    }

    /**
     * 通用改状态（供 UnitOfWork.registerRideStatusUpdate 使用）。
     * 会根据目标状态带上相应时间戳。
     */
    public int updateStatus(Connection conn, int rideId, String status) throws SQLException {
        final String s = status == null ? "" : status.toUpperCase();

        // 需要更新时间戳的列名（timestamptz）
        String timeCol = switch (s) {
            case "ACCEPTED"  -> "accepted_at";
            case "ENROUTE"   -> "started_at";
            case "COMPLETED" -> "completed_at";
            case "CANCELLED" -> "cancelled_at";
            default -> null;
        };

        final String sql = (timeCol == null)
                ? "UPDATE ride SET status = ? WHERE id = ?"
                : "UPDATE ride SET " + timeCol + " = ?, status = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (timeCol == null) {
                ps.setString(1, s);
                ps.setInt(2, rideId);
            } else {
                ps.setObject(1, OffsetDateTime.now(MEL)); // ★ 墨尔本时间
                ps.setString(2, s);
                ps.setInt(3, rideId);
            }
            return ps.executeUpdate();
        }
    }


    public List<Ride> findAllRequested(Connection conn) throws SQLException {
        final String sql =
                "SELECT id, rider_id, driver_id, pickup_postcode, dest_postcode, " +
                        "       fare, status, created_at " +
                        "  FROM ride " +
                        " WHERE status = 'REQUESTED' " +
                        " ORDER BY created_at DESC";

        List<Ride> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Ride r = new Ride();

                // 基本字段
                r.setId(rs.getInt("id"));
                r.setRiderId(rs.getInt("rider_id"));

                // driver_id 可能为 NULL（尚未分配司机）
                Integer driverId = (Integer) rs.getObject("driver_id");
                if (driverId != null) r.setDriverId(driverId);

                r.setPickup(rs.getString("pickup_postcode"));
                r.setDestination(rs.getString("dest_postcode"));

                // 金额（你的列是 numeric(12,2)；JDBC 读成 double/BigDecimal 都可）
                // 若你的 Ride 用 BigDecimal，请改成 getBigDecimal 与 setAmount(BigDecimal)
                r.setFare(rs.getDouble("fare"));

                r.setStatus(rs.getString("status"));

                // 只有 created_at，REQUESTED 下没有 requested_at
                Timestamp tsCreated = rs.getTimestamp("created_at");
                if (tsCreated != null) {
                    java.time.LocalDateTime t = tsCreated.toLocalDateTime();
                    long createdMs = t.atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();
                    r.setRequestedAt(createdMs);    // ✅ 传 long
                }


                list.add(r);
            }
        }
        return list;
    }


}
