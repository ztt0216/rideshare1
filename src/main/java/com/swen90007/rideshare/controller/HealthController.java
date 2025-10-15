package com.swen90007.rideshare.controller;

import jakarta.servlet.http.*;
import java.io.IOException;

/** Simple health endpoint for deployment monitoring (Render). */
public class HealthController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        resp.getWriter().println("OK");
    }
}
