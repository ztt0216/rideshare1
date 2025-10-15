package com.rideshare.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rideshare.domain.Ride;
import com.rideshare.domain.unitofwork.DatabaseUnitOfWork;
import com.rideshare.service.RideService;
import com.rideshare.service.RideServiceImpl;
import com.rideshare.util.RideShareException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@WebServlet("/api/rides/*")
public class RideController extends HttpServlet {
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        super.init();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // POST /api/rides - Request a new ride
                handleRequestRide(request, response);
            } else if (pathInfo.matches("/\\d+/accept")) {
                // POST /api/rides/{id}/accept - Driver accepts ride
                handleAcceptRide(request, response, pathInfo);
            } else if (pathInfo.matches("/\\d+/start")) {
                // POST /api/rides/{id}/start - Driver starts ride
                handleStartRide(request, response, pathInfo);
            } else if (pathInfo.matches("/\\d+/complete")) {
                // POST /api/rides/{id}/complete - Driver completes ride
                handleCompleteRide(request, response, pathInfo);
            } else if (pathInfo.matches("/\\d+/cancel")) {
                // POST /api/rides/{id}/cancel - Rider cancels ride
                handleCancelRide(request, response, pathInfo);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"Endpoint not found\"}");
            }
        } catch (RideShareException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error\"}");
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = request.getPathInfo();
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/rides - Get available rides (for drivers)
                handleGetAvailableRides(request, response);
            } else if (pathInfo.matches("/\\d+")) {
                // GET /api/rides/{id} - Get specific ride
                handleGetRide(request, response, pathInfo);
            } else if (pathInfo.matches("/rider/\\d+")) {
                // GET /api/rides/rider/{riderId} - Get rider history
                handleGetRiderHistory(request, response, pathInfo);
            } else if (pathInfo.matches("/driver/\\d+")) {
                // GET /api/rides/driver/{driverId} - Get driver history
                handleGetDriverHistory(request, response, pathInfo);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("{\"error\":\"Endpoint not found\"}");
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"Internal server error\"}");
            e.printStackTrace();
        }
    }

    private void handleRequestRide(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        BufferedReader reader = request.getReader();
        Map<String, Object> requestData = objectMapper.readValue(reader, Map.class);
        
        Long riderId = ((Number) requestData.get("riderId")).longValue();
        String pickupLocation = (String) requestData.get("pickupLocation");
        String destination = (String) requestData.get("destination");
        
        DatabaseUnitOfWork uow = new DatabaseUnitOfWork();
        RideService rideService = new RideServiceImpl(uow);
        
        Long rideId = rideService.requestRide(riderId, pickupLocation, destination);
        
        response.setStatus(HttpServletResponse.SC_CREATED);
        response.getWriter().write("{\"rideId\":" + rideId + ",\"message\":\"Ride requested successfully\"}");
    }

    private void handleAcceptRide(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        Long rideId = extractIdFromPath(pathInfo);
        
        BufferedReader reader = request.getReader();
        Map<String, Object> requestData = objectMapper.readValue(reader, Map.class);
        Long driverId = ((Number) requestData.get("driverId")).longValue();
        
        DatabaseUnitOfWork uow = new DatabaseUnitOfWork();
        RideService rideService = new RideServiceImpl(uow);
        
        rideService.acceptRide(rideId, driverId);
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"message\":\"Ride accepted successfully\"}");
    }

    private void handleStartRide(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        Long rideId = extractIdFromPath(pathInfo);
        
        BufferedReader reader = request.getReader();
        Map<String, Object> requestData = objectMapper.readValue(reader, Map.class);
        Long driverId = ((Number) requestData.get("driverId")).longValue();
        
        DatabaseUnitOfWork uow = new DatabaseUnitOfWork();
        RideService rideService = new RideServiceImpl(uow);
        
        rideService.startRide(rideId, driverId);
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"message\":\"Ride started successfully\"}");
    }

    private void handleCompleteRide(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        Long rideId = extractIdFromPath(pathInfo);
        
        BufferedReader reader = request.getReader();
        Map<String, Object> requestData = objectMapper.readValue(reader, Map.class);
        Long driverId = ((Number) requestData.get("driverId")).longValue();
        
        DatabaseUnitOfWork uow = new DatabaseUnitOfWork();
        RideService rideService = new RideServiceImpl(uow);
        
        rideService.completeRide(rideId, driverId);
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"message\":\"Ride completed successfully, payment processed\"}");
    }

    private void handleCancelRide(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        Long rideId = extractIdFromPath(pathInfo);
        
        BufferedReader reader = request.getReader();
        Map<String, Object> requestData = objectMapper.readValue(reader, Map.class);
        Long riderId = ((Number) requestData.get("riderId")).longValue();
        
        DatabaseUnitOfWork uow = new DatabaseUnitOfWork();
        RideService rideService = new RideServiceImpl(uow);
        
        rideService.cancelRide(rideId, riderId);
        
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("{\"message\":\"Ride cancelled successfully\"}");
    }

    private void handleGetAvailableRides(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        DatabaseUnitOfWork uow = new DatabaseUnitOfWork();
        RideService rideService = new RideServiceImpl(uow);
        
        List<Ride> rides = rideService.getAvailableRides();
        
        String json = objectMapper.writeValueAsString(rides);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(json);
    }

    private void handleGetRide(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        Long rideId = Long.parseLong(pathInfo.substring(1));
        
        DatabaseUnitOfWork uow = new DatabaseUnitOfWork();
        RideService rideService = new RideServiceImpl(uow);
        
        Ride ride = rideService.getRideById(rideId);
        
        if (ride == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\":\"Ride not found\"}");
        } else {
            String json = objectMapper.writeValueAsString(ride);
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(json);
        }
    }

    private void handleGetRiderHistory(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        Long riderId = Long.parseLong(pathInfo.substring(pathInfo.lastIndexOf("/") + 1));
        
        DatabaseUnitOfWork uow = new DatabaseUnitOfWork();
        RideService rideService = new RideServiceImpl(uow);
        
        List<Ride> rides = rideService.getRiderHistory(riderId);
        
        String json = objectMapper.writeValueAsString(rides);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(json);
    }

    private void handleGetDriverHistory(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        Long driverId = Long.parseLong(pathInfo.substring(pathInfo.lastIndexOf("/") + 1));
        
        DatabaseUnitOfWork uow = new DatabaseUnitOfWork();
        RideService rideService = new RideServiceImpl(uow);
        
        List<Ride> rides = rideService.getDriverHistory(driverId);
        
        String json = objectMapper.writeValueAsString(rides);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(json);
    }

    private Long extractIdFromPath(String pathInfo) {
        String[] parts = pathInfo.split("/");
        return Long.parseLong(parts[1]);
    }
}
