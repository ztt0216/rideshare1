package com.rideshare.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideshare.domain.User;
import com.rideshare.service.UserService;
import com.rideshare.service.UserServiceImpl;
import com.rideshare.util.RideShareException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/api/users/*")
public class UserController extends HttpServlet {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public UserController() {
        this.userService = new UserServiceImpl();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            // Handle registration
            try {
                RegisterRequest registerRequest = objectMapper.readValue(req.getReader(), RegisterRequest.class);
                User user = userService.register(
                    registerRequest.getName(),
                    registerRequest.getEmail(),
                    registerRequest.getPassword(),
                    registerRequest.getRole()
                );
                sendJsonResponse(resp, 201, user);
            } catch (RideShareException e) {
                sendErrorResponse(resp, 400, e.getMessage());
            }
        } else if (pathInfo.equals("/login")) {
            // Handle login
            try {
                LoginRequest loginRequest = objectMapper.readValue(req.getReader(), LoginRequest.class);
                User user = userService.login(loginRequest.getEmail(), loginRequest.getPassword());
                sendJsonResponse(resp, 200, user);
            } catch (RideShareException e) {
                sendErrorResponse(resp, 401, e.getMessage());
            }
        } else if (pathInfo.matches("/\\d+/wallet")) {
            // Handle wallet update
            try {
                Long userId = Long.parseLong(pathInfo.split("/")[1]);
                WalletRequest walletRequest = objectMapper.readValue(req.getReader(), WalletRequest.class);
                userService.updateWalletBalance(userId, walletRequest.getAmount());
                sendJsonResponse(resp, 200, "Wallet updated successfully");
            } catch (RideShareException e) {
                sendErrorResponse(resp, 400, e.getMessage());
            }
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            Long userId = Long.parseLong(pathInfo.substring(1));
            User user = userService.getUserById(userId);
            sendJsonResponse(resp, 200, user);
        } catch (RideShareException e) {
            sendErrorResponse(resp, 404, e.getMessage());
        } catch (NumberFormatException e) {
            sendErrorResponse(resp, 400, "Invalid user ID");
        }
    }

    private void sendJsonResponse(HttpServletResponse resp, int status, Object data) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        objectMapper.writeValue(resp.getOutputStream(), data);
    }

    private void sendErrorResponse(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        objectMapper.writeValue(resp.getOutputStream(), new ErrorResponse(message));
    }

    // Request/Response classes
    private static class RegisterRequest {
        private String name;
        private String email;
        private String password;
        private String role;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    private static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    private static class WalletRequest {
        private BigDecimal amount;

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() { return error; }
    }
}
