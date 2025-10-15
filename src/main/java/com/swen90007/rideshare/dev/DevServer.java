package com.swen90007.rideshare.dev;

import com.swen90007.rideshare.controller.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.servlet.DispatcherType;
import java.util.EnumSet;

/**
 * Embedded Jetty dev server.
 * 启动本地 HTTP 服务（默认 8080），并注册所有 Servlet 与 CORS Filter。
 * 仅用于本地开发，不影响 Render 上的 WAR 部署。
 */
public class DevServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("DEV_PORT", "8080"));
        Server server = new Server(port);

        // 创建上下文（/ 根路径）
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // === 注册所有控制器（与 web.xml 一致） ===
        context.addServlet(new ServletHolder(new AuthController()), "/api/auth/*");
        context.addServlet(new ServletHolder(new AvailabilityController()), "/api/availability/*");
        context.addServlet(new ServletHolder(new RideRiderController()), "/api/rider/ride/*");
        context.addServlet(new ServletHolder(new RideDriverController()), "/api/driver/ride/*");
        context.addServlet(new ServletHolder(new WalletController()), "/api/wallet/*");
        context.addServlet(new ServletHolder(new HistoryController()), "/api/history/*");
        context.addServlet(new ServletHolder(new HealthController()), "/api/health");

        // === 全局 CORS 过滤器（与 web.xml 一致） ===
        FilterHolder cors = new FilterHolder(new CorsFilter());
        context.addFilter(cors, "/*", EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR));

        // 简单首页
        context.addServlet(new ServletHolder(new jakarta.servlet.http.HttpServlet() {
            @Override protected void doGet(jakarta.servlet.http.HttpServletRequest req,
                                           jakarta.servlet.http.HttpServletResponse resp) throws java.io.IOException {
                resp.setContentType("text/html;charset=UTF-8");
                resp.getWriter().println("<h1>RideShare DevServer</h1><p>Try <a href=\"/api/health\">/api/health</a></p>");
            }
        }), "/");

        System.out.println("[DevServer] starting at http://localhost:" + port);
        server.start();
        server.join();
    }
}
