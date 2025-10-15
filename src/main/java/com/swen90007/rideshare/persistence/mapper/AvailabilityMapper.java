package com.swen90007.rideshare.persistence.mapper;

import com.swen90007.rideshare.persistence.DBConnection;

import java.sql.*;
import java.time.LocalTime;

/**
 * 司机可接单时间段 Mapper
 */
public class AvailabilityMapper {

    /**
     * 允许同一天插入多条；仅校验 start < end；写入包含 day_of_week 列
     * @param driverId   司机 id
     * @param dayOfWeek  1..7 (MON..SUN)
     * @param day        "MON".."SUN"（用于展示）
     * @param start      "HH:mm"
     * @param end        "HH:mm"
     */
    public void upsert(int driverId, int dayOfWeek, String day, String start, String end) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            // 解析时间并校验 start < end
            LocalTime s = LocalTime.parse(start); // "HH:mm"
            LocalTime e = LocalTime.parse(end);   // "HH:mm"
            if (!s.isBefore(e)) {
                throw new IllegalArgumentException("start must be before end");
            }

            conn = DBConnection.getConnection();

            // 允许同一天多条记录：使用 INSERT，不带 ON CONFLICT
            String sql = """
                    INSERT INTO availability(driver_id, day_of_week, day, start_time, end_time)
                    VALUES (?, ?, ?, ?, ?)
                    """;

            ps = conn.prepareStatement(sql);
            ps.setInt(1, driverId);
            ps.setInt(2, dayOfWeek);
            ps.setString(3, day);
            // 注意表里是 "time without time zone"，用 java.sql.Time 传入
            ps.setTime(4, Time.valueOf(s));
            ps.setTime(5, Time.valueOf(e));

            ps.executeUpdate();
            conn.commit();
        } catch (Exception ex) {
            // 发生异常回滚
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignore) {}
            }
            throw new RuntimeException(ex);
        } finally {
            closeQuietly(ps);
            DBConnection.closeThreadConnection();
        }
    }

    /** 供 Service/Controller 返回司机所有可接时间段（保持你工程里的返回格式） */
    public String findByDriverId(int driverId) {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = DBConnection.getConnection();
            String sql = """
                    SELECT id, driver_id, day_of_week, day, to_char(start_time,'HH24:MI') AS start,
                           to_char(end_time,'HH24:MI') AS end
                    FROM availability
                    WHERE driver_id = ?
                    ORDER BY day_of_week, start_time
                    """;
            ps = conn.prepareStatement(sql);
            ps.setInt(1, driverId);
            rs = ps.executeQuery();

            // 简单拼 JSON（如果你工程里已有工具方法，可以替换为现有实现）
            StringBuilder sb = new StringBuilder();
            sb.append("{\"driverId\":").append(driverId).append(",\"items\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(',');
                first = false;
                sb.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(',')
                        .append("\"dayOfWeek\":").append(rs.getInt("day_of_week")).append(',')
                        .append("\"day\":\"").append(rs.getString("day")).append("\",")
                        .append("\"start\":\"").append(rs.getString("start")).append("\",")
                        .append("\"end\":\"").append(rs.getString("end")).append("\"")
                        .append("}");
            }
            sb.append("]}");
            conn.commit();
            return sb.toString();
        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignore) {}
            }
            throw new RuntimeException(ex);
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            DBConnection.closeThreadConnection();
        }
    }

    /**
     * 供 RideService 校验某司机在 day("MON") 某时("HH:mm") 是否在可接时间段内
     * 规则：存在一条记录满足
     *   driver_id = ?
     *   AND day = ?
     *   AND start_time <= HH:mm AND HH:mm < end_time
     */
    public boolean isDriverAvailable(int driverId, String day, String hhmm) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            return isDriverAvailable(conn, driverId, day, hhmm);
        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignore) {}
            }
            throw new RuntimeException(ex);
        } finally {
            DBConnection.closeThreadConnection();
        }
    }

    /** 内部重载：复用已有连接（便于与 RideService 同一事务内使用） */
    public boolean isDriverAvailable(Connection conn, int driverId, String day, String hhmm) throws SQLException {
        String sql = """
                SELECT 1
                FROM availability
                WHERE driver_id = ?
                  AND day = ?
                  AND start_time <= ?
                  AND ? < end_time
                LIMIT 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            LocalTime t = LocalTime.parse(hhmm);
            Time tt = Time.valueOf(t);
            ps.setInt(1, driverId);
            ps.setString(2, day);
            ps.setTime(3, tt);
            ps.setTime(4, tt);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ---------------------- helpers ----------------------

    private static void closeQuietly(AutoCloseable c) {
        if (c != null) {
            try { c.close(); } catch (Exception ignore) {}
        }
    }
}
