package com.swen90007.rideshare.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swen90007.rideshare.service.RideService;
import com.swen90007.rideshare.persistence.unitofwork.UnitOfWork;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.util.Optional;

public class RideDriverController extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final RideService rideService = new RideService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = Optional.ofNullable(req.getPathInfo()).orElse("/");
        resp.setContentType("application/json;charset=UTF-8");

        try (Connection conn = UnitOfWork.getConnection()) {
            switch (path) {
                case "/accept" -> handleAccept(conn, req, resp);
                case "/begin"  -> handleBegin(conn, req, resp);
                case "/complete" -> handleComplete(conn, req, resp);
                case "/requestList" -> handleRequestList(conn, req, resp);
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

    private void handleAccept(Connection conn, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonNode body = readJsonIfAny(req);
        String rideIdStr   = firstNotNull(req.getParameter("rideId"), getText(body, "rideId"));
        String driverIdStr = firstNotNull(req.getParameter("driverId"), getText(body, "driverId"));

        if (rideIdStr == null || driverIdStr == null) {
            throw new IllegalArgumentException("Missing parameters: rideId / driverId");
        }
        int rideId = Integer.parseInt(rideIdStr);
        int driverId = Integer.parseInt(driverIdStr);

        rideService.acceptRide(conn, rideId, driverId);

        String ok = MAPPER.createObjectNode()
                .put("ok", true)
                .put("rideId", rideId)
                .put("driverId", driverId)
                .put("status", "ACCEPTED")
                .toString();
        resp.getWriter().println(ok);
    }

    private void handleBegin(Connection conn, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonNode body = readJsonIfAny(req);

        // 安全读取 rideId，不论是 JSON 还是 form-data
        String rideIdStr = firstNotNull(req.getParameter("rideId"), getText(body, "rideId"));
        if (rideIdStr == null) {
            throw new IllegalArgumentException("Missing parameter: rideId");
        }

//        System.out.println("riderId is " + rideIdStr);
        int rideId;
        try {
            // 尝试从字符串解析整数
            rideId = Integer.parseInt(rideIdStr.trim());
        } catch (NumberFormatException e) {
            try {
                // 如果字符串解析失败，再尝试当成 Long 转换
                long tmp = Long.parseLong(rideIdStr.trim());
                rideId = (int) tmp;
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid rideId type: " + rideIdStr);
            }
        }

        // 调用 service
        rideService.beginRide(conn, rideId);

        String ok = MAPPER.createObjectNode()
                .put("ok", true)
                .put("rideId", rideId)
                .put("message", "Ride begun")
                .toString();
        resp.getWriter().println(ok);
    }


    private void handleComplete(Connection conn, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonNode body = readJsonIfAny(req);
        String rideIdStr = firstNotNull(req.getParameter("rideId"), getText(body, "rideId"));
        if (rideIdStr == null) throw new IllegalArgumentException("Missing parameter: rideId");
        int rideId = Integer.parseInt(rideIdStr);

        rideService.completeRide(conn, rideId);

        String ok = MAPPER.createObjectNode()
                .put("ok", true)
                .put("rideId", rideId)
                .put("status", "COMPLETED")
                .toString();
        resp.getWriter().println(ok);
    }

    /** 新增：列出司机可接受的（符合可接单时间窗口的）REQUESTED 订单 */
    private void handleRequestList(Connection conn, HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // 仅接收 driverId，一个字段即可（与项目里其它接口的取参方式一致）
        String driverIdStr = firstNotNull(req.getParameter("driverId"), readJsonIfAny(req).get("driverId"));
        if (driverIdStr == null || driverIdStr.isBlank()) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Missing parameter: driverId");
            return;
        }
        int driverId = Integer.parseInt(driverIdStr.trim());

        ArrayNode items = rideService.listRequestableRides(conn, driverId);

        ObjectNode ok = MAPPER.createObjectNode()
                .put("ok", true)
                .set("rides", items);

        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().println(ok.toString());
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

    // ---------------- helpers（与项目里现有工具风格保持一致） ----------------

    private static String firstNotNull(String queryParam, JsonNode bodyField) {
        if (queryParam != null) return queryParam;
        if (bodyField != null && !bodyField.isNull()) return bodyField.asText();
        return null;
    }

    private JsonNode readJsonIfAny(HttpServletRequest req) {
        try {
            if (req.getInputStream() == null || req.getInputStream().available() == 0) return MAPPER.createObjectNode();
            return MAPPER.readTree(req.getInputStream());
        } catch (Exception ignore) {
            return MAPPER.createObjectNode();
        }
    }

    private void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        ObjectNode err = MAPPER.createObjectNode()
                .put("ok", false)
                .put("status", status)
                .put("error", message == null ? "unknown" : message);
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().println(err.toString());
    }
}
