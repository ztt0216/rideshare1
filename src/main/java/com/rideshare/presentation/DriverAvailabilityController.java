package com.rideshare.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rideshare.domain.DriverAvailability;
import com.rideshare.service.DriverAvailabilityService;
import com.rideshare.service.DriverAvailabilityServiceImpl;
import com.rideshare.util.RideShareException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/drivers/availability/*")
public class DriverAvailabilityController extends HttpServlet {
    private final DriverAvailabilityService availabilityService;
    private final ObjectMapper objectMapper;

    public DriverAvailabilityController() {
        this.availabilityService = new DriverAvailabilityServiceImpl();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        try {
            // Extract driver ID from path (/driverId)
            Long driverId = Long.parseLong(pathInfo.substring(1));
            
            // Parse request body
            SetAvailabilityRequest request = objectMapper.readValue(req.getReader(), SetAvailabilityRequest.class);
            
            availabilityService.setAvailability(driverId, request.getSchedules());
            
            sendJsonResponse(resp, 200, new SuccessResponse("Availability set successfully"));
        } catch (RideShareException e) {
            sendErrorResponse(resp, 400, e.getMessage());
        } catch (NumberFormatException e) {
            sendErrorResponse(resp, 400, "Invalid driver ID");
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
            // Extract driver ID from path (/driverId)
            Long driverId = Long.parseLong(pathInfo.substring(1));
            
            List<DriverAvailability> availabilities = availabilityService.getAvailability(driverId);
            
            sendJsonResponse(resp, 200, availabilities);
        } catch (RideShareException e) {
            sendErrorResponse(resp, 404, e.getMessage());
        } catch (NumberFormatException e) {
            sendErrorResponse(resp, 400, "Invalid driver ID");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        try {
            // Extract driver ID from path (/driverId)
            Long driverId = Long.parseLong(pathInfo.substring(1));
            
            availabilityService.clearAvailability(driverId);
            
            sendJsonResponse(resp, 200, new SuccessResponse("Availability cleared successfully"));
        } catch (RideShareException e) {
            sendErrorResponse(resp, 400, e.getMessage());
        } catch (NumberFormatException e) {
            sendErrorResponse(resp, 400, "Invalid driver ID");
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
    private static class SetAvailabilityRequest {
        private List<DriverAvailabilityService.AvailabilitySchedule> schedules;

        public List<DriverAvailabilityService.AvailabilitySchedule> getSchedules() {
            return schedules;
        }

        public void setSchedules(List<DriverAvailabilityService.AvailabilitySchedule> schedules) {
            this.schedules = schedules;
        }
    }

    private static class SuccessResponse {
        private final String message;

        public SuccessResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }
}
