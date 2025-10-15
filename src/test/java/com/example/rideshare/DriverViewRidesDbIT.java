package com.example.rideshare;

import com.example.rideshare.api.RideViewAdapter;
import com.example.rideshare.domain.DayOfWeek;
import com.example.rideshare.domain.RideStatus;
import com.example.rideshare.domain.Role;
import com.example.rideshare.repo.impl.JdbcRideRepository;
import com.example.rideshare.repo.AvailabilityRepository;
import com.example.rideshare.service.AvailabilityService;
import com.example.rideshare.service.EstimationService;
import com.example.rideshare.service.UserService;
import com.example.rideshare.service.impl.AvailabilityServiceImplement;
import com.example.rideshare.service.impl.UserServiceImplement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DB integration test (US4: View Rides for Driver)
 * - Uses JdbcRideRepository for real ride read/write operations
 * - Reads through RideViewAdapter and asserts inside/outside window and list content
 */
class DriverViewRidesDbIT {

    // ===== DB connection (env first, fallback to local defaults) =====
    private static final String JDBC_URL = System.getenv().getOrDefault(
            "JDBC_URL", "jdbc:postgresql://localhost:5432/postgres");
    private static final String JDBC_USER = System.getenv().getOrDefault("JDBC_USER", "postgre");
    private static final String JDBC_PASSWORD = System.getenv().getOrDefault("JDBC_PASSWORD", "123456");

    private Connection conn() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    // ===== Simple fixed-fee estimator for deterministic assertions =====
    static class FixedEstimator implements EstimationService {
        private final BigDecimal amount;
        FixedEstimator(BigDecimal amount) { this.amount = amount; }
        @Override public EstimatedRide estimate(com.example.rideshare.domain.Rider rider, int pickupPostcode, int destinationPostcode) {
            return new EstimatedRide(amount, 10);
        }
    }

    @Test
    void db_flow_inside_and_outside_window_and_list_content() throws Exception {
        // Unique prefix for later DB queries
        String token = "dbit_" + System.currentTimeMillis();

        // 1) Create users (real database insertion)
        UserService users = new UserServiceImplement();
        long driverId = users.registerUser(
                "DBIT Driver",
                token + "_driver@example.com",
                "secret123",
                Role.DRIVER
        );
        long riderId = users.registerUser(
                "DBIT Rider",
                token + "_rider@example.com",
                "secret123",
                Role.RIDER
        );
        assertTrue(driverId > 0, "driverId should be > 0");
        assertTrue(riderId > 0, "riderId should be > 0");

        // 2) Configure driver Tuesday 10:00-18:00 (real database insertion)
        AvailabilityService avail = new AvailabilityServiceImplement();
        avail.setWeeklySchedule(driverId, List.of(
                new AvailabilityRepository.Row(DayOfWeek.TUESDAY, LocalTime.of(10,0), LocalTime.of(18,0))
        ));

        // 3) Insert rides: two should be displayed (REQUESTED & unassigned), two interference
        LocalDateTime base = LocalDateTime.now().minusMinutes(20);
        long r1 = insertRide(riderId, null,      3053, 3000, base.plusMinutes(1),  RideStatus.REQUESTED.name(), null);                 // Display
        long r2 = insertRide(riderId, null,      3122, 3004, base.plusMinutes(2),  RideStatus.REQUESTED.name(), new BigDecimal("7.89"));// Display (has fare)
        long r3 = insertRide(riderId, driverId,  3999, 3000, base.plusMinutes(3),  RideStatus.REQUESTED.name(), null);                 // Interference: already assigned
        long r4 = insertRide(riderId, null,      3000, 3053, base.plusMinutes(4),  RideStatus.ACCEPTED.name(),  null);                 // Interference: not REQUESTED
        assertTrue(r1 > 0 && r2 > 0 && r3 > 0 && r4 > 0);

        // 4) outside window (Tuesday 08:00)
        LocalDateTime tue0800 = pickNext(DayOfWeek.TUESDAY)
                .withHour(8).withMinute(0).withSecond(0).withNano(0);

        RideViewAdapter adapter = new RideViewAdapter(new JdbcRideRepository(), new FixedEstimator(new BigDecimal("15.55")));
        var out = adapter.viewForDriver(driverId, tue0800);

        assertFalse(out.insideWindow, "should be outside availability window at Tue 08:00");
        assertNull(out.items, "outsideWindow should not return items");
        assertNotNull(out.message);
        assertTrue(out.message.toLowerCase().contains("outside"));

        // 5) inside window (Tuesday 11:00)
        LocalDateTime tue1100 = pickNext(DayOfWeek.TUESDAY)
                .withHour(11).withMinute(0).withSecond(0).withNano(0);

        var in = adapter.viewForDriver(driverId, tue1100);
        assertTrue(in.insideWindow, "should be inside availability window at Tue 11:00");
        assertNotNull(in.items);
        assertEquals(2, in.items.size());

        // Order: by requested_at ASC → r1 then r2
        assertEquals(r1, in.items.get(0).id);
        assertEquals("AUD 15.55", in.items.get(0).fareEstimate); // Estimator output
        assertEquals(r2, in.items.get(1).id);
        assertEquals("AUD 7.89", in.items.get(1).fareEstimate);  // Using existing fare
    }

    // ===== helper: insert a ride row and return generated id =====
    private long insertRide(long riderId, Long driverId, int pickupPostcode, int destPostcode,
                            LocalDateTime requestedAt, String status, BigDecimal fare) throws SQLException {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO rides (rider_id, driver_id, status, fare,
                                    pickup_postcode, destination_postcode, requested_at)
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

    // ===== helper: next occurrence of a domain DayOfWeek =====
    private static LocalDateTime pickNext(DayOfWeek d) {
        java.time.DayOfWeek jdk = java.time.DayOfWeek.valueOf(d.name());
        LocalDateTime now = LocalDateTime.now();
        int diff = jdk.getValue() - now.getDayOfWeek().getValue();
        if (diff < 0) diff += 7;
        return now.plusDays(diff);
    }
}
