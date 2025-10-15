package com.swen90007.rideshare.service;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.*;

/** JSON aggregation for rider/driver histories (JOIN ride + payment + users). */
public class HistoryService {

    public String listRiderHistory(Connection conn, int riderId) {
        String sql = "SELECT r.id, r.pickup, r.destination, r.status, r.fare, " +
                "p.timestamp AS payment_ts, u.name AS driver_name " +
                "FROM ride r " +
                "LEFT JOIN payment p ON p.ride_id = r.id " +
                "LEFT JOIN users u ON u.id = r.driver_id " +
                "WHERE r.rider_id = ? ORDER BY r.id DESC";
        return runHistoryQuery(conn, sql, riderId, true);
    }

    public String listDriverHistory(Connection conn, int driverId) {
        String sql = "SELECT r.id, r.pickup, r.destination, r.status, r.fare, " +
                "p.timestamp AS payment_ts, u.name AS rider_name " +
                "FROM ride r " +
                "LEFT JOIN payment p ON p.ride_id = r.id " +
                "LEFT JOIN users u ON u.id = r.rider_id " +
                "WHERE r.driver_id = ? ORDER BY r.id DESC";
        return runHistoryQuery(conn, sql, driverId, false);
    }

    private String runHistoryQuery(Connection conn, String sql, int userId, boolean riderView) {
        JSONArray arr = new JSONArray();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject o = new JSONObject();
                    o.put("rideId", rs.getInt("id"));
                    o.put("pickup", rs.getString("pickup"));
                    o.put("destination", rs.getString("destination"));
                    o.put("status", rs.getString("status"));
                    Object fare = rs.getObject("fare");
                    if (fare != null) o.put("fare", rs.getBigDecimal("fare"));
                    Object ts = rs.getObject("payment_ts");
                    if (ts != null) o.put("paymentTimestamp", rs.getLong("payment_ts"));
                    String name = rs.getString(riderView ? "driver_name" : "rider_name");
                    if (name != null) o.put(riderView ? "driverName" : "riderName", name);
                    arr.put(o);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return arr.toString();
    }
}
