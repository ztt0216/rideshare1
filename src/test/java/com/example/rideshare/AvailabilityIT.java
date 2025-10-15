package com.example.rideshare;

import com.example.rideshare.domain.DayOfWeek;
import com.example.rideshare.domain.Role;
import com.example.rideshare.repo.AvailabilityRepository;
import com.example.rideshare.service.AvailabilityService;
import com.example.rideshare.service.UserService;
import com.example.rideshare.service.impl.AvailabilityServiceImplement;
import com.example.rideshare.service.impl.UserServiceImplement;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AvailabilityIT {

    @Test
    void set_get_and_canAccept_logic() {
        // Preparation: create new DRIVER
        UserService users = new UserServiceImplement();
        long driverId = users.registerUser(
                "Avail Driver",
                "avail_driver_" + System.currentTimeMillis() + "@example.com",
                "secret123",
                Role.DRIVER
        );
        assertTrue(driverId > 0);

        AvailabilityService svc = new AvailabilityServiceImplement();

        // Set two time windows
        List<AvailabilityRepository.Row> rows = List.of(
                new AvailabilityRepository.Row(DayOfWeek.MONDAY,  LocalTime.of(9,0),  LocalTime.of(17,0)),
                new AvailabilityRepository.Row(DayOfWeek.TUESDAY, LocalTime.of(10,0), LocalTime.of(18,0))
        );
        svc.setWeeklySchedule(driverId, rows);

        // Read and verify
        var got = svc.getWeeklySchedule(driverId);
        assertEquals(2, got.size());
        assertEquals(DayOfWeek.MONDAY,  got.get(0).day());
        assertEquals(LocalTime.of(9,0), got.get(0).start());
        assertEquals(LocalTime.of(17,0),got.get(0).end());

        // Rule: configured => can only accept within window
        assertTrue(svc.canAcceptAt(driverId, DayOfWeek.MONDAY, LocalTime.of(9,30)));
        assertFalse(svc.canAcceptAt(driverId, DayOfWeek.MONDAY, LocalTime.of(18,0)));
        assertFalse(svc.canAcceptAt(driverId, DayOfWeek.SUNDAY, LocalTime.NOON));
    }

    @Test
    void no_schedule_means_accept_anytime() {
        UserService users = new UserServiceImplement();
        long driverId = users.registerUser(
                "NoSched Driver",
                "avail_nosched_" + System.currentTimeMillis() + "@example.com",
                "secret123",
                Role.DRIVER
        );
        AvailabilityService svc = new AvailabilityServiceImplement();

        // Not configured => can accept at any time
        assertTrue(svc.canAcceptAt(driverId, DayOfWeek.SATURDAY, LocalTime.of(3,0)));
        assertTrue(svc.canAcceptAt(driverId, DayOfWeek.MONDAY, LocalTime.of(23,59,59)));
    }

    @Test
    void overlapping_should_be_rejected() {
        UserService users = new UserServiceImplement();
        long driverId = users.registerUser(
                "Overlap Driver",
                "avail_overlap_" + System.currentTimeMillis() + "@example.com",
                "secret123",
                Role.DRIVER
        );

        AvailabilityService svc = new AvailabilityServiceImplement();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                svc.setWeeklySchedule(driverId, List.of(
                        new AvailabilityRepository.Row(DayOfWeek.MONDAY, LocalTime.of(9,0),  LocalTime.of(12,0)),
                        new AvailabilityRepository.Row(DayOfWeek.MONDAY, LocalTime.of(11,0), LocalTime.of(13,0)) // Overlap
                )));
        assertTrue(ex.getMessage().toLowerCase().contains("overlap"));
    }
}
