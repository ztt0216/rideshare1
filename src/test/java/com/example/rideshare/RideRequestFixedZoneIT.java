package com.example.rideshare;

import com.example.rideshare.domain.*;
import com.example.rideshare.repo.RideRepository;
import com.example.rideshare.service.impl.RideServiceImplement;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US8: Request Ride (Rider) —— Fixed zone pricing + balance validation + persistence field validation
 * Uses in-memory RideRepository to avoid dependencies on DB/frontend.
 *
 * Coverage points:
 *  - Airport trip: 3045 -> 60.00
 *  - Interstate trip: either end not 3xxx -> 500.00
 *  - Regional trip: either end in Victoria regional area (3300-3999) -> 220.00
 *  - Metro trip: others (3000-3299) -> 40.00
 *  - Insufficient balance blocking, no deduction, no order placed
 *  - Postcode must be 4 digits
 */
class RideRequestFixedZoneIT {

    /** In-memory implementation: simulates JDBC behavior and generates auto-increment id; implements acceptIfUnassigned to match interface signature. */
    static class InMemRideRepository implements RideRepository {
        private final Map<Long, Ride> store = new LinkedHashMap<>();
        private final AtomicLong seq = new AtomicLong(1);

        @Override
        public void save(Ride ride) {
            if (ride.getRideId() <= 0) {
                ride.setRideId(seq.getAndIncrement());
            }
            store.put(ride.getRideId(), cloneForStore(ride));
        }

        @Override
        public Optional<Ride> findById(long id) {
            Ride r = store.get(id);
            return Optional.ofNullable(cloneForReturn(r));
        }

        @Override
        public List<Ride> findAll() {
            return store.values().stream().map(this::cloneForReturn).toList();
        }

        /** Signature required by US5: only when ride is still REQUESTED and unassigned, accept and mark as ACCEPTED. */
        @Override
        public boolean acceptIfUnassigned(long rideId, long driverId) {
            Ride r = store.get(rideId);
            if (r == null) return false;
            if (r.getDriver() != null) return false;
            if (r.getStatus() != RideStatus.REQUESTED) return false;

            // Construct a minimal Driver placeholder (in domain, as long as non-null can be considered "assigned")
            Driver d = new Driver(
                    driverId,
                    "Driver-" + driverId,
                    "d" + driverId + "@example.com",
                    "hash",
                    new Wallet(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
            r.setDriver(d);
            r.setStatus(RideStatus.ACCEPTED);
            store.put(rideId, r);
            return true;
        }

        /** Prevent external references from modifying stored objects: shallow copy. */
        private Ride cloneForStore(Ride src) {
            if (src == null) return null;
            Ride r = new Ride();
            r.setRideId(src.getRideId());
            r.setPickupPostcode(src.getPickupPostcode());
            r.setDestinationPostcode(src.getDestinationPostcode());
            r.setRequestedAt(src.getRequestedAt());
            r.setRider(src.getRider());
            r.setDriver(src.getDriver());
            r.setStatus(src.getStatus());
            r.setFare(src.getFare());
            r.setPayment(src.getPayment());
            return r;
        }
        private Ride cloneForReturn(Ride src) { return cloneForStore(src); }
    }

    /** Convenience constructor: give Rider a wallet with specified balance. */
    private Rider riderWithBalance(BigDecimal amount) {
        Wallet w = new Wallet();
        w.setBalance(amount);
        return new Rider(100L, "R1", "r1@example.com", "hash", w, new ArrayList<>());
    }

    /** Common assertion: Ride is correctly written (status=fare etc). */
    private void assertRidePersistedBasic(Ride r,
                                          int pickup, int dest,
                                          BigDecimal expectedFare) {
        assertNotNull(r);
        assertTrue(r.getRideId() > 0, "rideId should be auto-increment greater than 0");
        assertEquals(RideStatus.REQUESTED, r.getStatus());
        assertEquals(pickup, r.getPickupPostcode());
        assertEquals(dest, r.getDestinationPostcode());
        assertEquals(expectedFare, r.getFare());
        assertNotNull(r.getRequestedAt());
    }

    // ===== Four fixed zone pricing rules =====

    @Test
    void airport_rule_overrides_to_60_and_deducts_wallet() {
        RideRepository repo = new InMemRideRepository();
        RideServiceImplement svc = new RideServiceImplement(repo);
        Rider rider = riderWithBalance(new BigDecimal("100.00"));

        int pickup = 3045; // Melbourne Airport
        int dest   = 3000;

        Ride r = svc.requestRide(rider, 0, pickup, dest, LocalDateTime.now());

        assertRidePersistedBasic(r, pickup, dest, new BigDecimal("60.00"));
        assertEquals(new BigDecimal("40.00"), rider.getWallet().getBalance()); // 100 - 60
    }

    @Test
    void interstate_is_500_if_any_side_not_in_victoria() {
        RideRepository repo = new InMemRideRepository();
        RideServiceImplement svc = new RideServiceImplement(repo);
        Rider rider = riderWithBalance(new BigDecimal("800.00"));

        int pickup = 2000; // Not 3xxx
        int dest   = 3000;

        Ride r = svc.requestRide(rider, 0, pickup, dest, LocalDateTime.now());
        assertEquals(new BigDecimal("500.00"), r.getFare());
        assertEquals(new BigDecimal("300.00"), rider.getWallet().getBalance()); // 800 - 500
    }

    @Test
    void regional_is_220_if_any_side_is_regional_victoria() {
        RideRepository repo = new InMemRideRepository();
        RideServiceImplement svc = new RideServiceImplement(repo);
        Rider rider = riderWithBalance(new BigDecimal("500.00"));

        int pickup = 3300; // Regional VIC
        int dest   = 3000;

        Ride r = svc.requestRide(rider, 0, pickup, dest, LocalDateTime.now());
        assertEquals(new BigDecimal("220.00"), r.getFare());
        assertEquals(new BigDecimal("280.00"), rider.getWallet().getBalance()); // 500 - 220
    }

    @Test
    void metro_is_40_otherwise() {
        RideRepository repo = new InMemRideRepository();
        RideServiceImplement svc = new RideServiceImplement(repo);
        Rider rider = riderWithBalance(new BigDecimal("100.00"));

        int pickup = 3053; // Metro 3000–3299
        int dest   = 3000;

        Ride r = svc.requestRide(rider, 0, pickup, dest, LocalDateTime.now());
        assertEquals(new BigDecimal("40.00"), r.getFare());
        assertEquals(new BigDecimal("60.00"), rider.getWallet().getBalance()); // 100 - 40
    }

    // ===== Balance/Input Validation =====

    @Test
    void should_fail_when_insufficient_funds() {
        RideRepository repo = new InMemRideRepository();
        RideServiceImplement svc = new RideServiceImplement(repo);
        Rider rider = riderWithBalance(new BigDecimal("39.99")); // Metro requires >= 40

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                svc.requestRide(rider, 0, 3000, 3001, LocalDateTime.now()));
        assertTrue(ex.getMessage().toLowerCase().contains("insufficient"),
                "Error message should contain insufficient (insufficient funds)");
        // Wallet balance should not change
        assertEquals(new BigDecimal("39.99"), rider.getWallet().getBalance());
    }

    @Test
    void should_reject_non_four_digit_postcodes() {
        RideRepository repo = new InMemRideRepository();
        RideServiceImplement svc = new RideServiceImplement(repo);
        Rider rider = riderWithBalance(new BigDecimal("100.00"));

        // Non-4-digit postcodes
        assertThrows(IllegalArgumentException.class,
                () -> svc.requestRide(rider, 0,  999, 3000, LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class,
                () -> svc.requestRide(rider, 0, 3000,10000, LocalDateTime.now()));
    }
}
