package com.swen90007.rideshare.controller;

import com.swen90007.rideshare.persistence.unitofwork.UnitOfWork;
import com.swen90007.rideshare.service.HistoryService;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;

/**
 * History endpoints (JSON):
 *  - GET /api/history/rider?userId=…
 *  - GET /api/history/driver?userId=…
 */
public class HistoryController extends HttpServlet {

    private final HistoryService historyService = new HistoryService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getPathInfo();
        if (path == null) { resp.sendError(404); return; }

        try (Connection conn = UnitOfWork.getConnection()) {
            String json;
            if (path.startsWith("/rider")) {
                int riderId = Integer.parseInt(req.getParameter("userId"));
                json = historyService.listRiderHistory(conn, riderId);
            } else if (path.startsWith("/driver")) {
                int driverId = Integer.parseInt(req.getParameter("userId"));
                json = historyService.listDriverHistory(conn, driverId);
            } else {
                resp.sendError(404);
                return;
            }
            conn.commit();
            resp.setContentType("application/json;charset=UTF-8");
            resp.getWriter().write(json);
        } catch (Exception e) {
            resp.setStatus(500);
            e.printStackTrace(resp.getWriter());
        }
    }
}
