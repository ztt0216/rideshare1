package com.example.rideshare;

import com.example.rideshare.domain.Role;
import com.example.rideshare.service.UserService;
import com.example.rideshare.service.impl.UserServiceImplement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRegisterIT {

    @Test
    void register_rider_persists_and_returns_id() {
        // Arrange
        UserService svc = new UserServiceImplement();
        String email = "test_reg_rider_" + System.currentTimeMillis() + "@example.com";

        // Act
        long id = svc.registerUser("Test Rider", email, "secret123", Role.RIDER);

        // Assert
        assertTrue(id > 0, "generated id should be > 0");
    }

    @Test
    void register_driver_persists_and_returns_id() {
        UserService svc = new UserServiceImplement();
        String email = "test_reg_driver_" + System.currentTimeMillis() + "@example.com";

        long id = svc.registerUser("Test Driver", email, "secret123", Role.DRIVER);

        assertTrue(id > 0, "generated id should be > 0");
    }

    @Test
    void duplicate_email_should_throw() {
        UserService svc = new UserServiceImplement();
        String email = "dup_" + System.currentTimeMillis() + "@example.com";

        long first = svc.registerUser("Dup1", email, "secret123", Role.RIDER);
        assertTrue(first > 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> svc.registerUser("Dup2", email, "secret123", Role.RIDER));
        assertTrue(ex.getMessage().toLowerCase().contains("email"),
                "should mention email");
    }
}
