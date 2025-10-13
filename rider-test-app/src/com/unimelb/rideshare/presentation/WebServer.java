package com.unimelb.rideshare.presentation;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.unimelb.rideshare.ApplicationContext;
import com.unimelb.rideshare.common.Result;
import com.unimelb.rideshare.domain.model.Driver;
import com.unimelb.rideshare.domain.model.Ride;
import com.unimelb.rideshare.domain.model.RideRequest;
import com.unimelb.rideshare.domain.model.RideStatus;
import com.unimelb.rideshare.domain.model.Rider;
import com.unimelb.rideshare.domain.value.AvailabilityWindow;
import com.unimelb.rideshare.domain.value.Location;
import com.unimelb.rideshare.domain.value.Vehicle;
import com.unimelb.rideshare.mapper.DriverDataMapper;
import com.unimelb.rideshare.mapper.RideDataMapper;
import com.unimelb.rideshare.mapper.RideRequestDataMapper;
import com.unimelb.rideshare.mapper.RiderDataMapper;
import com.unimelb.rideshare.service.DriverService;
import com.unimelb.rideshare.service.RideService;
import com.unimelb.rideshare.service.RiderService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Minimal HTTP server that exposes the service layer for front-end testing without introducing
 * external dependencies. Endpoints accept simple form-encoded payloads for ease of use.
 */
public final class WebServer {
    private static final int PORT = 8080;

    private final DriverService driverService;
    private final RiderService riderService;
    private final RideService rideService;
    private final DriverDataMapper driverMapper;
    private final RiderDataMapper riderMapper;
    private final RideRequestDataMapper rideRequestMapper;
    private final RideDataMapper rideMapper;

    private HttpServer server;

    public WebServer(ApplicationContext context) {
        this.driverService = context.getDriverService();
        this.riderService = context.getRiderService();
        this.rideService = context.getRideService();
        this.driverMapper = context.getDriverMapper();
        this.riderMapper = context.getRiderMapper();
        this.rideRequestMapper = context.getRideRequestMapper();
        this.rideMapper = context.getRideMapper();
    }

    public void start() throws IOException {
        if (server != null) {
            return;
        }
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/drivers", withCors(this::handleDrivers));
        server.createContext("/api/drivers/register", withCors(this::handleDriverRegister));
        server.createContext("/api/drivers/availability", withCors(this::handleDriverAvailability));
        server.createContext("/api/riders/register", withCors(this::handleRiderRegister));
        server.createContext("/api/rides/request", withCors(this::handleRideRequest));
        server.createContext("/api/rides/match", withCors(this::handleRideMatch));
        server.createContext("/api/rides/accept", withCors(this::handleRideAccept));
        server.createContext("/api/rides/start", withCors(this::handleRideStart));
        server.createContext("/api/rides/complete", withCors(this::handleRideComplete));
        server.createContext("/api/rides/cancel", withCors(this::handleRideCancel));
        server.createContext("/api/rides/open", withCors(this::handleOpenRides));
        server.createContext("/api/rides/active", withCors(this::handleActiveRides));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("HTTP server started on http://localhost:" + PORT + "/api/...\n");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private HttpHandler withCors(HttpHandler delegate) {
        return exchange -> {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            headers.add("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 204, "");
                return;
            }
            delegate.handle(exchange);
        };
    }

    private void handleDrivers(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        List<Driver> drivers = driverMapper.findAll();
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < drivers.size(); i++) {
            Driver driver = drivers.get(i);
            json.append('{')
                    .append("\"id\":\"").append(driver.getId()).append("\",")
                    .append("\"name\":\"").append(escape(driver.getName())).append("\",")
                    .append("\"email\":\"").append(escape(driver.getEmail())).append("\"");
            if (driver.getVehicle() != null) {
                json.append(',')
                        .append("\"vehicle\":\"")
                        .append(escape(driver.getVehicle().toString()))
                        .append("\"");
            }
            json.append('}');
            if (i < drivers.size() - 1) {
                json.append(',');
            }
        }
        json.append(']');
        sendJson(exchange, 200, json.toString());
    }

    private void handleDriverRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> form = parseForm(exchange);
        String name = form.get("name");
        String email = form.get("email");
        String make = form.get("make");
        String model = form.get("model");
        String colour = form.get("colour");
        String registration = form.get("registration");
        Result<Driver> result = driverService.registerDriver(
                name,
                email,
                new Vehicle(make, model, colour, registration)
        );
        if (result.isSuccess()) {
            Driver driver = result.getValue().orElseThrow();
            sendJson(exchange, 201, "{\"id\":\"" + driver.getId() + "\"}");
        } else {
            sendJson(exchange, 400, errorJson(result.getMessage().orElse("Unable to register driver")));
        }
    }

    private void handleDriverAvailability(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> form = parseForm(exchange);
        try {
            UUID driverId = UUID.fromString(required(form, "driverId"));
            DayOfWeek day = DayOfWeek.valueOf(required(form, "day").toUpperCase());
            LocalTime start = LocalTime.parse(required(form, "start"));
            LocalTime end = LocalTime.parse(required(form, "end"));
            Result<Driver> result = driverService.updateAvailability(
                    driverId,
                    List.of(new AvailabilityWindow(day, start, end))
            );
            if (result.isSuccess()) {
                sendJson(exchange, 200, "{\"status\":\"updated\"}");
            } else {
                sendJson(exchange, 400, errorJson(result.getMessage().orElse("Unable to update availability")));
            }
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, errorJson(ex.getMessage()));
        }
    }

    private void handleRiderRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> form = parseForm(exchange);
        Result<Rider> result = riderService.registerRider(form.get("name"), form.get("email"));
        if (result.isSuccess()) {
            Rider rider = result.getValue().orElseThrow();
            sendJson(exchange, 201, "{\"id\":\"" + rider.getId() + "\"}");
        } else {
            sendJson(exchange, 400, errorJson(result.getMessage().orElse("Unable to register rider")));
        }
    }

    private void handleRideRequest(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> form = parseForm(exchange);
        try {
            UUID riderId = UUID.fromString(required(form, "riderId"));
            Location pickup = Location.builder().description(required(form, "pickup")).build();
            Location dropOff = Location.builder().description(required(form, "dropOff")).build();
            Result<RideRequest> result = rideService.requestRide(riderId, pickup, dropOff);
            if (result.isSuccess()) {
                RideRequest rideRequest = result.getValue().orElseThrow();
                sendJson(exchange, 201, "{\"id\":\"" + rideRequest.getId() + "\"}");
            } else {
                sendJson(exchange, 400, errorJson(result.getMessage().orElse("Unable to create ride request")));
            }
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, errorJson(ex.getMessage()));
        }
    }

    private void handleRideMatch(HttpExchange exchange) throws IOException {
        handleRideTransition(exchange, "rideRequestId", id -> rideService.matchRide(id), 200);
    }

    private void handleRideAccept(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> form = parseForm(exchange);
        try {
            UUID rideId = UUID.fromString(required(form, "rideId"));
            UUID driverId = UUID.fromString(required(form, "driverId"));
            Result<Ride> result = rideService.acceptRide(rideId, driverId);
            respondWithRide(exchange, result);
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, errorJson(ex.getMessage()));
        }
    }

    private void handleRideStart(HttpExchange exchange) throws IOException {
        handleRideTransition(exchange, "rideId", rideService::startRide, 200);
    }

    private void handleRideComplete(HttpExchange exchange) throws IOException {
        handleRideTransition(exchange, "rideId", rideService::completeRide, 200);
    }

    private void handleRideCancel(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> form = parseForm(exchange);
        try {
            UUID rideRequestId = UUID.fromString(required(form, "rideRequestId"));
            Result<RideRequest> result = rideService.cancelRequest(rideRequestId);
            if (result.isSuccess()) {
                sendJson(exchange, 200, "{\"status\":\"cancelled\"}");
            } else {
                sendJson(exchange, 400, errorJson(result.getMessage().orElse("Unable to cancel request")));
            }
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, errorJson(ex.getMessage()));
        }
    }

    private void handleOpenRides(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        List<RideRequest> requests = rideRequestMapper.findAll();
        List<RideRequest> open = new ArrayList<>();
        for (RideRequest request : requests) {
            if (request.getStatus() == RideStatus.REQUESTED || request.getStatus() == RideStatus.MATCHED) {
                open.add(request);
            }
        }
        sendJson(exchange, 200, rideRequestsJson(open));
    }

    private void handleActiveRides(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        List<Ride> rides = rideMapper.findAll();
        List<Ride> active = new ArrayList<>();
        for (Ride ride : rides) {
            if (ride.getStatus() != RideStatus.COMPLETED && ride.getStatus() != RideStatus.CANCELLED) {
                active.add(ride);
            }
        }
        sendJson(exchange, 200, ridesJson(active));
    }

    private void handleRideTransition(HttpExchange exchange,
                                      String idField,
                                      java.util.function.Function<UUID, Result<Ride>> handler,
                                      int successStatus) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendMethodNotAllowed(exchange);
            return;
        }
        Map<String, String> form = parseForm(exchange);
        try {
            UUID id = UUID.fromString(required(form, idField));
            Result<Ride> result = handler.apply(id);
            respondWithRide(exchange, result, successStatus);
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, errorJson(ex.getMessage()));
        }
    }

    private void respondWithRide(HttpExchange exchange, Result<Ride> result) throws IOException {
        respondWithRide(exchange, result, 200);
    }

    private void respondWithRide(HttpExchange exchange, Result<Ride> result, int successStatus) throws IOException {
        if (result.isSuccess()) {
            Ride ride = result.getValue().orElseThrow();
            sendJson(exchange, successStatus, rideJson(ride));
        } else {
            sendJson(exchange, 400, errorJson(result.getMessage().orElse("Operation failed")));
        }
    }

    private Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String rawBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> params = new HashMap<>();
        if (rawBody.isBlank()) {
            return params;
        }
        for (String pair : rawBody.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    private String required(Map<String, String> params, String key) {
        String value = params.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing parameter: " + key);
        }
        return value;
    }

    private void sendMethodNotAllowed(HttpExchange exchange) throws IOException {
        sendJson(exchange, 405, errorJson("Method not allowed"));
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        sendResponse(exchange, status, json);
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String errorJson(String message) {
        return "{\"error\":\"" + escape(message) + "\"}";
    }

    private String rideJson(Ride ride) {
        return new StringBuilder("{")
                .append("\"id\":\"").append(ride.getId()).append("\",")
                .append("\"rideRequestId\":\"").append(ride.getRideRequestId()).append("\",")
                .append("\"driverId\":\"").append(ride.getDriverId()).append("\",")
                .append("\"riderId\":\"").append(ride.getRiderId()).append("\",")
                .append("\"status\":\"").append(ride.getStatus()).append("\"")
                .append("}")
                .toString();
    }

    private String ridesJson(List<Ride> rides) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < rides.size(); i++) {
            json.append(rideJson(rides.get(i)));
            if (i < rides.size() - 1) {
                json.append(',');
            }
        }
        return json.append(']').toString();
    }

    private String rideRequestsJson(List<RideRequest> requests) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < requests.size(); i++) {
            RideRequest request = requests.get(i);
            json.append('{')
                    .append("\"id\":\"").append(request.getId()).append("\",")
                    .append("\"riderId\":\"").append(request.getRiderId()).append("\",")
                    .append("\"status\":\"").append(request.getStatus()).append("\"")
                    .append('}');
            if (i < requests.size() - 1) {
                json.append(',');
            }
        }
        return json.append(']').toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}