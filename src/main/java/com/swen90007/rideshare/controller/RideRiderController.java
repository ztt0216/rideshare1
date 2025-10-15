package com.swen90007.rideshare.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swen90007.rideshare.service.RideService;
import com.swen90007.rideshare.persistence.unitofwork.UnitOfWork;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.util.Optional;

public class RideRiderController extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RideService rideService = new RideService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = Optional.ofNullable(req.getPathInfo()).orElse("/");
        resp.setContentType("application/json;charset=UTF-8");

        try (Connection conn = UnitOfWork.getConnection()) {
            switch (path) {
                case "/request" -> handleRequest(conn, req, resp);
                case "/cancel"  -> handleCancel(conn, req, resp);
                default -> {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    resp.getWriter().println("{\"ok\":false,\"error\":\"unknown endpoint\"}");
                }
            }
            conn.commit();
        } catch (IllegalArgumentException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println(jsonError(e.getMessage(), 400));
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().println(jsonError(e.getMessage(), 500));
        }
    }

    private void handleRequest(Connection conn, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonNode body = readJsonIfAny(req);

        String riderIdStr = firstNotNull(req.getParameter("riderId"), getText(body, "riderId"));
        String pickup = firstNotNull(req.getParameter("pickup_postcode"), getText(body, "pickup_postcode"));
        String dest = firstNotNull(req.getParameter("destination_postcode"), getText(body, "destination_postcode"));

        if (riderIdStr == null || pickup == null || dest == null) {
            throw new IllegalArgumentException("Missing parameters: riderId / pickup_postcode / destination_postcode");
        }

        int riderId = Integer.parseInt(riderIdStr);
        int rideId = rideService.requestRideWithBalanceCheck(conn, riderId, pickup, dest);

        String ok = MAPPER.createObjectNode()
                .put("ok", true)
                .put("rideId", rideId)
                .put("riderId", riderId)
                .put("pickup_postcode", pickup)
                .put("destination_postcode", dest)
                .toString();
        resp.getWriter().println(ok);
    }

    private void handleCancel(Connection conn, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonNode body = readJsonIfAny(req);
        String rideIdStr = firstNotNull(req.getParameter("rideId"), getText(body, "rideId"));
        if (rideIdStr == null) throw new IllegalArgumentException("Missing parameter: rideId");
        int rideId = Integer.parseInt(rideIdStr);

        rideService.cancelRide(conn, rideId);

        String ok = MAPPER.createObjectNode()
                .put("ok", true)
                .put("rideId", rideId)
                .put("message", "Ride cancelled")
                .toString();
        resp.getWriter().println(ok);
    }

    private JsonNode readJsonIfAny(HttpServletRequest req) {
        try {
            if (req.getContentType() != null && req.getContentType().toLowerCase().contains("application/json")) {
                return MAPPER.readTree(req.getInputStream());
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String getText(JsonNode node, String field) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) return null;
        return node.get(field).asText();
    }

    private String firstNotNull(String a, String b) { return a != null ? a : b; }

    private String jsonError(String msg, int status) {
        return MAPPER.createObjectNode()
                .put("ok", false)
                .put("error", msg)
                .put("status", status)
                .toString();
    }
}