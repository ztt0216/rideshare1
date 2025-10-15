// src/test/java/com/example/rideshare/DriverAcceptRideDbIT.java
package com.example.rideshare;

import com.example.rideshare.domain.DayOfWeek;
import com.example.rideshare.domain.Role;
import com.example.rideshare.domain.RideStatus;
import com.example.rideshare.service.AvailabilityService;
import com.example.rideshare.service.UserService;
import com.example.rideshare.service.impl.AvailabilityServiceImplement;
import com.example.rideshare.service.impl.DriverRideServiceImplement;
import com.example.rideshare.service.impl.UserServiceImplement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DriverAcceptRideDbIT {

    private static final String JDBC_URL = System.getenv().getOrDefault(
            "JDBC_URL", "jdbc:postgresql://localhost:5432/postgres");
    private static final String JDBC_USER = System.getenv().getOrDefault("JDBC_USER", "postgre");
    private static final String JDBC_PASSWORD = System.getenv().getOrDefault("JDBC_PASSWORD", "123456");

    private Connection conn() throws SQLException { return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD); }

    @Test
    void accept_success_inside_window_and_atomic() throws Exception {
        String token = "acc_" + System.currentTimeMillis();

        // Prepare users
        UserService users = new UserServiceImplement();
        long driverId = users.registerUser("ACC Driver", token + "_driver@example.com", "secret123", Role.DRIVER);
        long riderId  = users.registerUser("ACC Rider",  token + "_rider@example.com",  "secret123", Role.RIDER);

        // Set window (Tuesday 10-18)
        AvailabilityService avail = new AvailabilityServiceImplement();
        avail.setWeeklySchedule(driverId, List.of(
                new com.example.rideshare.repo.AvailabilityRepository.Row(DayOfWeek.TUESDAY, LocalTime.of(10,0), LocalTime.of(18,0))
        ));

        // Insert a REQUESTED unassigned order
        LocalDateTime reqAt = LocalDateTime.now().minusMinutes(3);
        long rideId = insertRide(riderId, null, 3000, 3053, reqAt, RideStatus.REQUESTED.name(), new BigDecimal("12.34"));

        // Accept ride on Tuesday 11:00
        LocalDateTime tue11 = pickNext(DayOfWeek.TUESDAY).withHour(11).withMinute(0).withSecond(0).withNano(0);
        new DriverRideServiceImplement().acceptRide(driverId, rideId, tue11);

        // Verify DB: driver_id assigned, status=ACCEPTED
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("""
                SELECT driver_id, status FROM rides WHERE id = ?
             """)) {
            ps.setLong(1, rideId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals(driverId, rs.getLong("driver_id"));
                assertEquals("ACCEPTED", rs.getString("status"));
            }
        }
    }

    @Test
    void accept_outside_window_should_fail() throws Exception {
        String token = "acc_out_" + System.currentTimeMillis();

        UserService users = new UserServiceImplement();
        long driverId = users.registerUser("ACC OUT Driver", token + "_driver@example.com", "secret123", Role.DRIVER);
        long riderId  = users.registerUser("ACC OUT Rider",  token + "_rider@example.com",  "secret123", Role.RIDER);

        AvailabilityService avail = new AvailabilityServiceImplement();
        avail.setWeeklySchedule(driverId, List.of(
                new com.example.rideshare.repo.AvailabilityRepository.Row(DayOfWeek.TUESDAY, LocalTime.of(10,0), LocalTime.of(18,0))
        ));

        long rideId = insertRide(riderId, null, 3000, 3053, LocalDateTime.now().minusMinutes(2), RideStatus.REQUESTED.name(), null);

        // Try to accept ride Tuesday 08:00 → should throw exception
        LocalDateTime tue08 = pickNext(DayOfWeek.TUESDAY).withHour(8).withMinute(0).withSecond(0).withNano(0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new DriverRideServiceImplement().acceptRide(driverId, rideId, tue08));
        assertTrue(ex.getMessage().toLowerCase().contains("window"));
    }

    @Test
    void accept_twice_should_only_first_win() throws Exception {
        String token = "acc_twice_" + System.currentTimeMillis();

        UserService users = new UserServiceImplement();
        long d1 = users.registerUser("D1", token + "_d1@example.com", "secret123", Role.DRIVER);
        long d2 = users.registerUser("D2", token + "_d2@example.com", "secret123", Role.DRIVER);
        long riderId = users.registerUser("R",  token + "_r@example.com",  "secret123", Role.RIDER);

        AvailabilityService avail = new AvailabilityServiceImplement();
        avail.setWeeklySchedule(d1, List.of(new com.example.rideshare.repo.AvailabilityRepository.Row(DayOfWeek.TUESDAY, LocalTime.of(0,0), LocalTime.of(23,59,59))));
        avail.setWeeklySchedule(d2, List.of(new com.example.rideshare.repo.AvailabilityRepository.Row(DayOfWeek.TUESDAY, LocalTime.of(0,0), LocalTime.of(23,59,59))));

        long rideId = insertRide(riderId, null, 3000, 3053, LocalDateTime.now().minusMinutes(2), RideStatus.REQUESTED.name(), null);
        LocalDateTime tue11 = pickNext(DayOfWeek.TUESDAY).withHour(11).withMinute(0).withSecond(0).withNano(0);

        // D1 accepts successfully
        new DriverRideServiceImplement().acceptRide(d1, rideId, tue11);

        // D2 tries to accept same ride → should fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new DriverRideServiceImplement().acceptRide(d2, rideId, tue11));
        assertTrue(ex.getMessage().toLowerCase().contains("no longer"));
    }

    // src/test/java/com/example/rideshare/DriverAcceptRideDbIT.java
    @Test
    void accept_without_any_schedule_should_fail() throws Exception {
        String token = "acc_nosched_" + System.currentTimeMillis();

        var users = new com.example.rideshare.service.impl.UserServiceImplement();
        long driverId = users.registerUser("NoSched Driver", token + "_d@example.com", "secret123",
                com.example.rideshare.domain.Role.DRIVER);
        long riderId  = users.registerUser("NoSched Rider",  token + "_r@example.com", "secret123",
                com.example.rideshare.domain.Role.RIDER);

        // Note: don't configure any availability (don't call setWeeklySchedule)

        long rideId = insertRide(riderId, null, 3000, 3053,
                java.time.LocalDateTime.now().minusMinutes(2),
                com.example.rideshare.domain.RideStatus.REQUESTED.name(), null);

        var svc = new com.example.rideshare.service.impl.DriverRideServiceImplement();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> svc.acceptRide(driverId, rideId, java.time.LocalDateTime.now()));
        assertTrue(ex.getMessage().toLowerCase().contains("no availability"));
    }


    // ---------- helpers ----------

    private long insertRide(long riderId, Long driverId, int pickupPostcode, int destPostcode,
                            LocalDateTime requestedAt, String status, BigDecimal fare) throws SQLException {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO rides (rider_id, driver_id, status, fare, pickup_postcode, destination_postcode, requested_at)
                 VALUES (?, ?, ?, ?, ?, ?, ?)
                 RETURNING id
             """)) {
            ps.setLong(1, riderId);
            if (driverId == null) ps.setNull(2, Types.INTEGER); else ps.setLong(2, driverId);
            ps.setString(3, status);
            if (fare == null) ps.setNull(4, Types.NUMERIC); else ps.setBigDecimal(4, fare);
            ps.setInt(5, pickupPostcode);
            ps.setInt(6, destPostcode);
            ps.setTimestamp(7, Timestamp.valueOf(requestedAt));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        throw new SQLException("insertRide failed");
    }

    private static LocalDateTime pickNext(DayOfWeek d) {
        java.time.DayOfWeek jdk = java.time.DayOfWeek.valueOf(d.name());
        LocalDateTime now = LocalDateTime.now();
        int diff = jdk.getValue() - now.getDayOfWeek().getValue();
        if (diff < 0) diff += 7;
        return now.plusDays(diff);
    }
}
