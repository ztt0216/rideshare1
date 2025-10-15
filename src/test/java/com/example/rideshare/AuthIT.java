package com.example.rideshare;

import com.example.rideshare.domain.Role;
import com.example.rideshare.service.AuthService;
import com.example.rideshare.service.UserService;
import com.example.rideshare.service.impl.AuthServiceImplement;
import com.example.rideshare.service.impl.UserServiceImplement;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthIT {

    @Test
    void login_rider_success_and_me_returns_role() {
        UserService users = new UserServiceImplement();
        String email = "auth_rider_" + System.currentTimeMillis() + "@example.com";
        String rawPwd = "secret123";
        users.registerUser("Auth Rider", email, rawPwd, Role.RIDER);

        AuthService auth = new AuthServiceImplement();
        String token = auth.login(email, rawPwd);
        assertNotNull(token);
        assertFalse(token.isBlank());

        AuthService.Me me = auth.me(token);
        assertEquals(Role.RIDER, me.role());
        assertEquals(email, me.email());
        assertTrue(me.userId() > 0);
    }

    @Test
    void login_driver_success_and_authorization_checks() {
        UserService users = new UserServiceImplement();
        String email = "auth_driver_" + System.currentTimeMillis() + "@example.com";
        String rawPwd = "secret123";
        users.registerUser("Auth Driver", email, rawPwd, Role.DRIVER);

        AuthService auth = new AuthServiceImplement();
        String token = auth.login(email, rawPwd);
        assertNotNull(token);

        // DRIVER accesses DRIVER resource OK
        assertDoesNotThrow(() -> auth.ensureRole(token, Role.DRIVER));
        // DRIVER accesses RIDER resource Forbidden
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> auth.ensureRole(token, Role.RIDER));
        assertEquals("Forbidden", ex.getMessage());
    }

    @Test
    void wrong_password_should_fail_login() {
        UserService users = new UserServiceImplement();
        String email = "auth_wrong_" + System.currentTimeMillis() + "@example.com";
        users.registerUser("Auth Wrong", email, "secret123", Role.RIDER);

        AuthService auth = new AuthServiceImplement();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> auth.login(email, "badpass"));
        assertTrue(ex.getMessage().toLowerCase().contains("invalid"));
    }

    @Test
    void logout_should_invalidate_session() {
        UserService users = new UserServiceImplement();
        String email = "auth_logout_" + System.currentTimeMillis() + "@example.com";
        String rawPwd = "secret123";
        users.registerUser("Auth Logout", email, rawPwd, Role.RIDER);

        AuthService auth = new AuthServiceImplement();
        String token = auth.login(email, rawPwd);
        auth.logout(token);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> auth.me(token));
        assertEquals("Not authenticated", ex.getMessage());
    }
}
