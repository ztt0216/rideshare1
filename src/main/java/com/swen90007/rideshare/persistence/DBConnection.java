package com.swen90007.rideshare.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Centralized PostgreSQL connection manager (Render version, hard-coded).
 *
 * This version keeps the connection fixed to your Render database:
 *   jdbc:postgresql://dpg-d3j0dmmmcj7s739iru1g-a.singapore-postgres.render.com/rideshare_xxqq
 *
 * Username: rider_share
 * Password: OXwKOkTJzzuXiG4Pt7Hnw2MAqj4jXgSb
 *
 * - getConnection(): thread-bound transactional connection (autoCommit=false)
 * - openConnection(): short-lived connection (autoCommit=true)
 */
public class DBConnection {

    private static final String JDBC_URL =
            "jdbc:postgresql://dpg-d3j0dmmmcj7s739iru1g-a.singapore-postgres.render.com/rideshare_xxqq?sslmode=require";
    private static final String USER = "rider_share";
    private static final String PASS = "OXwKOkTJzzuXiG4Pt7Hnw2MAqj4jXgSb";

    private static final ThreadLocal<Connection> TL = new ThreadLocal<>();

    /** Thread-bound transactional connection (autoCommit=false). */
    public static Connection getConnection() throws SQLException {
        Connection c = TL.get();
        if (c == null || c.isClosed()) {
            c = DriverManager.getConnection(JDBC_URL, USER, PASS);
            c.setAutoCommit(false);
            TL.set(c);
        }

        // ★ 关键：固定会话时区为墨尔本（PostgreSQL）
        try (Statement st = c.createStatement()) {
            st.execute("SET TIME ZONE 'Australia/Melbourne'");
        }
        return c;
    }

    /** One-off short connection for simple read (autoCommit=true). */
    public static Connection openConnection() throws SQLException {
        Connection c = DriverManager.getConnection(JDBC_URL, USER, PASS);
        c.setAutoCommit(true);

//        // ★ 关键：固定会话时区为墨尔本（PostgreSQL）
//        try (Statement st = c.createStatement()) {
//            st.execute("SET TIME ZONE 'Australia/Melbourne'");
//        }

        return c;
    }

    /** Close and clear thread-bound connection if any. */
    public static void closeThreadConnection() {
        Connection c = TL.get();
        if (c != null) {
            try { c.close(); } catch (Exception ignore) {}
            TL.remove();
        }
    }
}
