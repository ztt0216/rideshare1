package com.swen90007.rideshare.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swen90007.rideshare.domain.model.User;
import com.swen90007.rideshare.service.UserService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * AuthController
 * 路由：
 *   POST /api/auth/register
 *   POST /api/auth/login
 *
 * 说明：
 * - 使用 jakarta.*（而非 javax.*）
 * - 注册/登录都同时支持 query/form 与 JSON body 两种提交方式
 * - 登录成功后在 HttpSession 中写入 "uid" 与 "role"
 */
public class AuthController extends HttpServlet {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getPathInfo(); // 期望为 /register 或 /login
        if (path == null) path = "";

        try {
            switch (path) {
                case "/register" -> handleRegister(req, resp);
                case "/login"    -> handleLogin(req, resp);
                default -> {
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    writeJson(resp, MAPPER.createObjectNode()
                            .put("ok", false)
                            .put("status", 404)
                            .put("error", "Unknown path: " + path));
                }
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeJson(resp, MAPPER.createObjectNode()
                    .put("ok", false)
                    .put("status", 500)
                    .put("error", e.getMessage()));
        }
    }

    /* --------------------------- 注册 --------------------------- */

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonNode body = readJsonIfAny(req);

        String email    = firstNotNull(req.getParameter("email"),    getText(body, "email"));
        String password = firstNotNull(req.getParameter("password"), getText(body, "password"));
        String name     = firstNotNull(req.getParameter("name"),     getText(body, "name"));
        String role     = firstNotNull(req.getParameter("role"),     getText(body, "role"));

        if (isBlank(email) || isBlank(password) || isBlank(name) || isBlank(role)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, MAPPER.createObjectNode()
                    .put("ok", false)
                    .put("status", 400)
                    .put("error", "Missing parameter: email/name/password/role"));
            return;
        }

        // 可选择做简单去重校验
        var existing = userService.findByEmail(email);
        if (existing != null) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            writeJson(resp, MAPPER.createObjectNode()
                    .put("ok", false)
                    .put("status", 409)
                    .put("error", "Email already registered"));
            return;
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(password);
        user.setName(name);
        user.setRole(role);

        userService.register(user); // 内部已负责事务提交

        resp.setStatus(HttpServletResponse.SC_CREATED);
        writeJson(resp, MAPPER.createObjectNode()
                .put("id", user.getId())
                .put("name", user.getName())
                .put("email", user.getEmail())
                .put("role", user.getRole()));
    }

    /* --------------------------- 登录 --------------------------- */

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonNode body = readJsonIfAny(req);

        String email    = firstNotNull(req.getParameter("email"),    getText(body, "email"));
        String password = firstNotNull(req.getParameter("password"), getText(body, "password"));

        if (isBlank(email) || isBlank(password)) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            writeJson(resp, MAPPER.createObjectNode()
                    .put("ok", false)
                    .put("status", 400)
                    .put("error", "Missing parameter: email/password"));
            return;
        }

        User user = userService.findByEmail(email);
        if (user == null || !password.equals(user.getPassword())) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            writeJson(resp, MAPPER.createObjectNode()
                    .put("ok", false)
                    .put("status", 401)
                    .put("error", "Invalid email or password"));
            return;
        }

        // 登录成功，写 Session
        HttpSession session = req.getSession(true);
        session.setAttribute("uid", user.getId());
        session.setAttribute("role", user.getRole());

        resp.setStatus(HttpServletResponse.SC_OK);
        writeJson(resp, MAPPER.createObjectNode()
                .put("ok", true)
                .put("userId", user.getId())
                .put("name", user.getName())
                .put("email", user.getEmail())
                .put("role", user.getRole()));
    }

    /* --------------------------- 工具方法 --------------------------- */

    private static JsonNode readJsonIfAny(HttpServletRequest req) {
        try {
            String contentType = req.getContentType();
            if (contentType != null && contentType.toLowerCase().contains("application/json")) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = req.getReader()) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
                if (!sb.isEmpty()) {
                    return MAPPER.readTree(sb.toString());
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    private static String getText(JsonNode body, String name) {
        if (body == null || body.isNull()) return null;
        JsonNode n = body.get(name);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private static String firstNotNull(String a, String b) {
        return a != null ? a : b;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static void writeJson(HttpServletResponse resp, JsonNode node) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try (PrintWriter w = resp.getWriter()) {
            w.write(MAPPER.writeValueAsString(node));
        }
    }
}
