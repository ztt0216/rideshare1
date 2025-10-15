package com.swen90007.rideshare.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.swen90007.rideshare.domain.model.Wallet;
import com.swen90007.rideshare.service.WalletService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Wallet API (jakarta.servlet):
 *
 *  GET  /api/wallet/{userId}               -> 查询余额（元）
 *  POST /api/wallet/topup                  -> 充值（元）
 *     - JSON: {"userId":1, "amount":2000}
 *     - x-www-form-urlencoded: userId=1&amount=2000
 *
 * 仅使用“amount”（单位元），不再使用 *_cents 字段。
 */
public class WalletController extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WalletService walletService = new WalletService();

    /* ---------- GET /api/wallet/{userId} ---------- */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        Integer userId = parseUserIdFromPath(req.getPathInfo());
        if (userId == null) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "userId missing in path");
            return;
        }

        try {
            Wallet w = walletService.get(userId);
            ObjectNode json = MAPPER.createObjectNode();
            json.put("ok", true);
            json.put("userId", w.getUserId());
            json.put("balance", w.getBalance()); // 元
            writeJson(resp, json);
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /* ---------- POST /api/wallet/topup ---------- */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String path = req.getPathInfo();
        if (path == null || !"/topup".equals(path)) {
            writeError(resp, HttpServletResponse.SC_NOT_FOUND, "Unsupported path");
            return;
        }

        Integer userId = null;
        Double amount = null;

        // 优先 JSON
        String ct = req.getContentType();
        if (ct != null && ct.toLowerCase().contains("application/json")) {
            try {
                JsonNode node = MAPPER.readTree(req.getInputStream());
                if (node != null) {
                    if (node.has("userId")) userId = node.get("userId").asInt();
                    if (node.has("amount")) amount = node.get("amount").asDouble();
                }
            } catch (Exception ignore) { /* fall back to form */ }
        }

        // 兼容 x-www-form-urlencoded
        if (userId == null && req.getParameter("userId") != null) {
            try { userId = Integer.parseInt(req.getParameter("userId")); } catch (NumberFormatException ignore) {}
        }
        if (amount == null && req.getParameter("amount") != null) {
            try { amount = Double.parseDouble(req.getParameter("amount")); } catch (NumberFormatException ignore) {}
        }

        if (userId == null || amount == null) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "userId/amount missing");
            return;
        }

        try {
            Wallet w = walletService.topUpAndReturn(userId, amount);
            ObjectNode json = MAPPER.createObjectNode();
            json.put("ok", true);
            json.put("message", "topup success");
            json.put("userId", w.getUserId());
            json.put("balance", w.getBalance()); // 元
            writeJson(resp, json);
        } catch (IllegalArgumentException e) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /* ================= helpers ================= */

    private static Integer parseUserIdFromPath(String path) {
        if (path == null) return null;
        String p = path.startsWith("/") ? path.substring(1) : path;
        if (p.isEmpty()) return null;
        try { return Integer.parseInt(p); }
        catch (NumberFormatException ignore) { return null; }
    }

    private static void writeJson(HttpServletResponse resp, ObjectNode node) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json; charset=UTF-8");
        resp.getWriter().write(node.toString());
    }

    private static void writeError(HttpServletResponse resp, int code, String message) throws IOException {
        resp.setStatus(code);
        ObjectNode node = MAPPER.createObjectNode();
        node.put("ok", false);
        node.put("error", message);
        node.put("status", code);
        writeJson(resp, node);
    }
}
