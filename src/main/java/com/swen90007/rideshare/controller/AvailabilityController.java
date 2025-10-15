package com.swen90007.rideshare.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swen90007.rideshare.service.AvailabilityService;
import com.swen90007.rideshare.service.UserService;
import com.swen90007.rideshare.domain.model.User;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AvailabilityController extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final AvailabilityService availabilityService = new AvailabilityService();
    private final UserService userService = new UserService();

    /** POST /api/availability  接收 JSON: { "driver_id":1, "day_of_week":2, "start_time":"08:00", "end_time":"12:00" } */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1) 解析 JSON
        var body = req.getReader().lines().reduce(new StringBuilder(),
                StringBuilder::append, StringBuilder::append).toString();
        ObjectNode json;
        try {
            json = (ObjectNode) MAPPER.readTree(body);
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON");
            return;
        }

        Integer driverId = json.has("driver_id") ? json.get("driver_id").asInt() : null;
        Integer dayOfWeek = json.has("day_of_week") ? json.get("day_of_week").asInt() : null;
        String startStr = json.has("start_time") ? json.get("start_time").asText() : null;
        String endStr   = json.has("end_time")   ? json.get("end_time").asText()   : null;

        if (driverId == null || dayOfWeek == null || startStr == null || endStr == null) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing parameters");
            return;
        }

        // 2) 基础校验
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "day_of_week must be 1..7");
            return;
        }
        if (!isHm(startStr) || !isHm(endStr)) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "time must be HH:mm");
            return;
        }
        if (startStr.compareTo(endStr) >= 0) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "start_time must be before end_time");
            return;
        }

        // 3) 仅 DRIVER 可设置
        User u = null;
        try {
            u = userService.findById(driverId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (u == null) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "User not found");
            return;
        }
        if (!"DRIVER".equalsIgnoreCase(u.getRole())) {
            writeError(resp, HttpServletResponse.SC_FORBIDDEN, "Only DRIVER can set availability");
            return;
        }

        // 4) 生成 day 字符串并调用 service
        String day = toDayString(dayOfWeek);
        try {
            availabilityService.upsertAvailability(driverId, dayOfWeek, day, startStr, endStr);
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            return;
        }

        // 5) 返回 OK
        ObjectNode ok = MAPPER.createObjectNode()
                .put("ok", true)
                .put("driver_id", driverId)
                .put("day_of_week", dayOfWeek)
                .put("day", day)
                .put("start_time", startStr)
                .put("end_time", endStr);
        writeJson(resp, HttpServletResponse.SC_OK, ok);
    }

    /** GET /api/availability/{driverId}  返回该司机的可用时段（JSON 字符串） */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        Integer driverId = null;
        String path = req.getPathInfo(); // e.g. "/2"
        if (path != null && path.length() > 1) {
            try { driverId = Integer.parseInt(path.substring(1)); } catch (NumberFormatException ignore) {}
        }
        if (driverId == null) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "driverId missing");
            return;
        }
        String json = null;
        try {
            json = availabilityService.getAvailabilityJson(driverId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.getWriter().write(json != null ? json : "[]");
    }

    // ---- helpers ----

    private static boolean isHm(String s) {
        return s != null && s.matches("\\d{2}:\\d{2}");
    }

    /** 1..7 -> MON..SUN */
    private static String toDayString(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "MON";
            case 2 -> "TUE";
            case 3 -> "WED";
            case 4 -> "THU";
            case 5 -> "FRI";
            case 6 -> "SAT";
            case 7 -> "SUN";
            default -> "MON";
        };
    }

    private static void writeError(HttpServletResponse resp, int code, String message) throws IOException {
        ObjectNode err = MAPPER.createObjectNode()
                .put("ok", false)
                .put("error", message)
                .put("status", code);
        writeJson(resp, code, err);
    }

    private static void writeJson(HttpServletResponse resp, int code, ObjectNode obj) throws IOException {
        resp.setStatus(code);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json");
        resp.getWriter().write(obj.toString());
    }
}
