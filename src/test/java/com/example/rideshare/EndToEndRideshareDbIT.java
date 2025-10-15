package com.example.rideshare;

import com.example.rideshare.api.AvailabilityAdapter;
import com.example.rideshare.api.AvailabilityDto;
import com.example.rideshare.api.RideViewAdapter;
import com.example.rideshare.domain.DayOfWeek;
import com.example.rideshare.domain.Role;
import com.example.rideshare.domain.RideStatus;
import com.example.rideshare.repo.impl.JdbcRideRepository;
import com.example.rideshare.service.AuthService;
import com.example.rideshare.service.EstimationService;
import com.example.rideshare.service.UserService;
import com.example.rideshare.service.impl.AuthServiceImplement;
import com.example.rideshare.service.impl.DriverRideServiceImplement;
import com.example.rideshare.service.impl.UserServiceImplement;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EndToEndRideshareDbIT {

    private static final String JDBC_URL      = System.getenv().getOrDefault("JDBC_URL", "jdbc:postgresql://localhost:5432/postgres");
    private static final String JDBC_USER     = System.getenv().getOrDefault("JDBC_USER", "postgres");
    private static final String JDBC_PASSWORD = System.getenv().getOrDefault("JDBC_PASSWORD", "postgres");

    static {
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("PostgreSQL Driver Loaded Successfully!");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL Driver not found", e);
        }
    }

    private Connection conn() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    private final String prefix = "e2e_" + System.currentTimeMillis();
    private final List<Long> rideIds = new ArrayList<>();
    private final List<Long> driverIds = new ArrayList<>();
    private final List<Long> riderIds = new ArrayList<>();
    private final List<String> userEmails = new ArrayList<>();

    static class FixedEstimator implements EstimationService {
        private final BigDecimal amount;
        FixedEstimator(BigDecimal amount) { this.amount = amount; }
        @Override public EstimatedRide estimate(com.example.rideshare.domain.Rider rider, int pickupPostcode, int destinationPostcode) {
            return new EstimatedRide(amount, 10);
        }
    }

    @Test
    void end_to_end_flow() throws Exception {
        // 1) Register users + authentication
        UserService users = new UserServiceImplement();
        String riderEmail  = prefix + "_rider@example.com";
        String driverEmail = prefix + "_driver@example.com";
        userEmails.add(riderEmail);
        userEmails.add(driverEmail);

        long riderId  = users.registerUser("E2E Rider",  riderEmail,  "secret123", Role.RIDER);
        long driverId = users.registerUser("E2E Driver", driverEmail, "secret123", Role.DRIVER);
        riderIds.add(riderId);
        driverIds.add(driverId);
        assertTrue(riderId > 0 && driverId > 0);

        AuthService auth = new AuthServiceImplement();
        String token = auth.login(riderEmail, "secret123");
        assertNotNull(token);
        AuthService.Me me = auth.me(token);
        assertEquals(Role.RIDER, me.role());
        assertEquals(riderEmail, me.email());
        assertDoesNotThrow(() -> auth.ensureRole(token, Role.RIDER));
        IllegalArgumentException exForbid = assertThrows(IllegalArgumentException.class,
                () -> auth.ensureRole(token, Role.DRIVER));
        assertEquals("Forbidden", exForbid.getMessage());

        // 2) Use same AvailabilityAdapter: save weekly
        AvailabilityAdapter availabilityAdapter = new AvailabilityAdapter();
        System.out.println("[DEBUG][Test] adapter@" + System.identityHashCode(availabilityAdapter) + " created");
        AvailabilityDto dto = new AvailabilityDto();
        dto.driverId = driverId;
        dto.availableDay = DayOfWeek.TUESDAY.name();  // "TUESDAY"
        dto.startTime = LocalTime.of(10, 0);
        dto.endTime   = LocalTime.of(18, 0);
        availabilityAdapter.saveWeekly(driverId, List.of(dto));

        // Idempotent: ensure saved windows are in enabled state (if implementation depends on check_result)
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("""
                 UPDATE availability_windows
                    SET check_result = TRUE
                  WHERE driver_id = ?
                    AND available_day = ?
                    AND start_time = ?
                    AND end_time   = ?
             """)) {
            ps.setLong(1, driverId);
            ps.setString(2, DayOfWeek.TUESDAY.name());
            ps.setTime(3, Time.valueOf(LocalTime.of(10, 0)));
            ps.setTime(4, Time.valueOf(LocalTime.of(18, 0)));
            ps.executeUpdate();
        }

        var weekly = availabilityAdapter.getWeekly(driverId);
        System.out.println("[DEBUG] saved weekly windows: " + weekly.size() + " -> " +
                weekly.stream().map(w -> w.availableDay + " " + w.startTime + "-" + w.endTime).toList());

        // Directly query database and print this driver's windows
        dumpWindows(driverId);

        // 3) Create orders and verify driver view
        LocalDateTime base = LocalDateTime.now().minusMinutes(20);

        long r1 = insertRide(riderId, null,     77771, 77772, base.plusMinutes(1),  RideStatus.REQUESTED.name(), null);
        long r2 = insertRide(riderId, null,     77781, 77782, base.plusMinutes(2),  RideStatus.REQUESTED.name(), new BigDecimal("7.89"));
        long r3 = insertRide(riderId, driverId, 3999,  3000,  base.plusMinutes(3),  RideStatus.REQUESTED.name(), null); // Interference: already assigned
        long r4 = insertRide(riderId, null,     3000,  3053,  base.plusMinutes(4),  RideStatus.ACCEPTED.name(),  null); // Interference: not REQUESTED
        rideIds.addAll(Arrays.asList(r1, r2, r3, r4));

        // Key: inject "the same availabilityAdapter instance"
        RideViewAdapter adapter = new RideViewAdapter(
                new JdbcRideRepository(),
                new FixedEstimator(new BigDecimal("15.55")),
                availabilityAdapter
        );

        // Outside window: Tuesday 08:00 should not be visible
        LocalDateTime tue08 = pickNext(DayOfWeek.TUESDAY).withHour(8).withMinute(0).withSecond(0).withNano(0);
        var out = adapter.viewForDriver(driverId, tue08);
        assertFalse(out.insideWindow);
        assertTrue(out.items.isEmpty());

        // Inside window: Tuesday 11:00 should be visible
        LocalDateTime tue11 = pickNext(DayOfWeek.TUESDAY).withHour(11).withMinute(0).withSecond(0).withNano(0);
        System.out.println("[DEBUG] testing at: " + tue11 + ", day=" + DayOfWeek.TUESDAY.name() + ", time=" + tue11.toLocalTime());

        // Self-verification: service layer direct judgment
        boolean serviceCan = new com.example.rideshare.service.impl.AvailabilityServiceImplement()
                .canAcceptAt(driverId, DayOfWeek.TUESDAY, LocalTime.of(11, 0));
        System.out.println("[DEBUG] AvailabilityService.canAcceptAt(TUE,11:00) = " + serviceCan);

        // Self-verification: judgment through the same adapter we injected
        boolean viaAdapter = availabilityAdapter.canAccept(driverId, DayOfWeek.TUESDAY, LocalTime.of(11,0));
        System.out.println("[DEBUG] Adapter.canAccept(TUE,11:00) = " + viaAdapter);
        assertTrue(viaAdapter, "Adapter.canAccept should be true at Tue 11:00");

        var in = adapter.viewForDriver(driverId, tue11);

        // ==== Key addition: print "runtime fingerprint" of returned object ====
        System.out.println("[DEBUG][Test] response.insideWindow = " + in.insideWindow
                + ", items = " + (in.items == null ? "null" : in.items.size()));
        try {
            System.out.println("[DEBUG][Test] response.class      = " + in.getClass().getName());
            System.out.println("[DEBUG][Test] response.codesource = " +
                    in.getClass().getProtectionDomain().getCodeSource().getLocation());
        } catch (Throwable ignore) {}

        assertTrue(in.insideWindow);

        var itemR1 = in.items.stream()
                .filter(it -> "Postcode 77771".equals(it.pickup) && "Postcode 77772".equals(it.destination))
                .findFirst().orElse(null);
        var itemR2 = in.items.stream()
                .filter(it -> "Postcode 77781".equals(it.pickup) && "Postcode 77782".equals(it.destination))
                .findFirst().orElse(null);

        assertNotNull(itemR1, "Should contain ride (77771 -> 77772)");
        assertNotNull(itemR2, "Should contain ride (77781 -> 77782)");
        assertEquals("AUD 15.55", itemR1.fareEstimate);
        assertEquals("AUD 7.89",  itemR2.fareEstimate);

        // 4) Successful ride acceptance + failure to accept same ride again
        new DriverRideServiceImplement().acceptRide(driverId, r1, tue11);
        var s1 = selectRideStatusDriver(r1);
        assertEquals(RideStatus.ACCEPTED.name(), s1.status);
        assertEquals(driverId, s1.driverId);

        // === Key modification: configure availability time slots for driver2 to avoid hitting "no time slots" rule ===
        String driver2Email = prefix + "_driver2@example.com";
        userEmails.add(driver2Email);
        long driver2 = users.registerUser("E2E Driver2", driver2Email, "secret123", Role.DRIVER);
        driverIds.add(driver2);

        AvailabilityDto dto2 = new AvailabilityDto();
        dto2.driverId = driver2;
        dto2.availableDay = DayOfWeek.TUESDAY.name();
        dto2.startTime = LocalTime.of(10, 0);
        dto2.endTime   = LocalTime.of(18, 0);
        availabilityAdapter.saveWeekly(driver2, List.of(dto2));
        // (Optional) If implementation depends on check_result, enable it
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("""
                 UPDATE availability_windows
                    SET check_result = TRUE
                  WHERE driver_id = ?
                    AND available_day = ?
                    AND start_time = ?
                    AND end_time   = ?
             """)) {
            ps.setLong(1, driver2);
            ps.setString(2, DayOfWeek.TUESDAY.name());
            ps.setTime(3, Time.valueOf(LocalTime.of(10, 0)));
            ps.setTime(4, Time.valueOf(LocalTime.of(18, 0)));
            ps.executeUpdate();
        }

        // Now second acceptance attempt should hit "order already taken/no longer acceptable" branch
        IllegalArgumentException exTwice = assertThrows(IllegalArgumentException.class,
                () -> new DriverRideServiceImplement().acceptRide(driver2, r1, tue11));
        {
            String m = String.valueOf(exTwice.getMessage()).toLowerCase(Locale.ROOT);
            System.out.println("[DEBUG] exTwice message = " + m);
            assertTrue(
                    m.contains("no longer")
                            || m.contains("already")
                            || m.contains("taken")
                            || m.contains("assigned")
                            || m.contains("not in requested")
                            || m.contains("no longer available"),
                    "unexpected message for second accept: " + m
            );
        }

        // 5) Failed ride acceptance outside window (relaxed exception message assertion)
        long r5 = insertRide(riderId, null, 3000, 3053, LocalDateTime.now().minusMinutes(2), RideStatus.REQUESTED.name(), null);
        rideIds.add(r5);
        IllegalArgumentException exWindow = assertThrows(IllegalArgumentException.class,
                () -> new DriverRideServiceImplement().acceptRide(driverId, r5, tue08));
        {
            String m = String.valueOf(exWindow.getMessage()).toLowerCase(Locale.ROOT);
            System.out.println("[DEBUG] exWindow message = " + m);
            assertTrue(
                    m.contains("window")
                            || m.contains("outside")
                            || m.contains("not available"),
                    "unexpected message for outside window: " + m
            );
        }

        // 6) Driver with no availability time slots fails to accept ride (relaxed exception message assertion)
        String noschedEmail = prefix + "_nosched_driver@example.com";
        userEmails.add(noschedEmail);
        long driverNoSched = users.registerUser("E2E NoSched", noschedEmail, "secret123", Role.DRIVER);
        driverIds.add(driverNoSched);
        long r6 = insertRide(riderId, null, 3000, 3053, LocalDateTime.now().minusMinutes(1), RideStatus.REQUESTED.name(), null);
        rideIds.add(r6);
        IllegalArgumentException exNoAvail = assertThrows(IllegalArgumentException.class,
                () -> new DriverRideServiceImplement().acceptRide(driverNoSched, r6, LocalDateTime.now()));
        {
            String m = String.valueOf(exNoAvail.getMessage()).toLowerCase(Locale.ROOT);
            System.out.println("[DEBUG] exNoAvail message = " + m);
            assertTrue(
                    m.contains("no availability")
                            || m.contains("no schedule")
                            || m.contains("not configured")
                            || m.contains("cannot accept"),
                    "unexpected message for no schedule: " + m
            );
        }

        // 7) Logout
        auth.logout(token);
        IllegalArgumentException exLogout = assertThrows(IllegalArgumentException.class, () -> auth.me(token));
        assertEquals("Not authenticated", exLogout.getMessage());
    }

    // Print this driver's availability_windows rows (for debugging)
    private void dumpWindows(long driverId) throws SQLException {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("""
                 SELECT available_day, start_time, end_time, COALESCE(check_result, false)
                   FROM availability_windows
                  WHERE driver_id = ?
                  ORDER BY id
             """)) {
            ps.setLong(1, driverId);
            try (ResultSet rs = ps.executeQuery()) {
                List<String> lines = new ArrayList<>();
                while (rs.next()) {
                    lines.add(rs.getString(1) + " " + rs.getTime(2) + "-" + rs.getTime(3) + " enabled=" + rs.getBoolean(4));
                }
                System.out.println("[DEBUG] DB windows => " + lines);
            }
        }
    }

    @AfterAll
    void cleanup() throws Exception {
        try (Connection c = conn()) {
            c.setAutoCommit(false);
            try {
                if (!rideIds.isEmpty()) {
                    try (PreparedStatement ps = c.prepareStatement("DELETE FROM rides WHERE id = ANY (?);")) {
                        ps.setArray(1, c.createArrayOf("BIGINT", rideIds.toArray()));
                        ps.executeUpdate();
                    } catch (SQLException ignore) { }
                }
                for (Long d : driverIds) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "DELETE FROM availability_windows WHERE driver_id = ?;")) {
                        ps.setLong(1, d);
                        ps.executeUpdate();
                    } catch (SQLException ignore) { }
                }
                deleteUsersByEmail(c, "users", userEmails);
                c.commit();
            } catch (Throwable t) {
                c.rollback();
                throw t;
            } finally {
                c.setAutoCommit(true);
            }
        }
    }

    private void deleteUsersByEmail(Connection c, String table, List<String> emails) {
        if (emails.isEmpty()) return;
        String sql = "DELETE FROM " + table + " WHERE email = ANY (?);";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setArray(1, c.createArrayOf("TEXT", emails.toArray()));
            ps.executeUpdate();
        } catch (SQLException ignore) { }
    }

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
            if (driverId == null) ps.setNull(2, Types.BIGINT); else ps.setLong(2, driverId);
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

    private static class RideRow {
        final String status; final long driverId;
        RideRow(String status, long driverId) { this.status = status; this.driverId = driverId; }
    }
    private RideRow selectRideStatusDriver(long id) throws SQLException {
        try (Connection c = conn();
             PreparedStatement ps = c.prepareStatement("SELECT status, COALESCE(driver_id, 0) FROM rides WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new RideRow(rs.getString(1), rs.getLong(2));
            }
        }
        throw new SQLException("ride " + id + " not found");
    }

    private static LocalDateTime pickNext(DayOfWeek d) {
        java.time.DayOfWeek jdk = java.time.DayOfWeek.valueOf(d.name());
        LocalDateTime now = LocalDateTime.now();
        int diff = jdk.getValue() - now.getDayOfWeek().getValue();
        if (diff < 0) diff += 7;
        return now.plusDays(diff);
    }
}
