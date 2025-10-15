package com.rideshare.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.net.URI;
import java.net.URISyntaxException;

public class DatabaseConfig {
    // Default values for local development
    private static final String DEFAULT_URL = "jdbc:postgresql://dpg-d3ne6rjipnbc73b1a8a0-a.oregon-postgres.render.com/rideshare1";
    private static final String DEFAULT_USER = "rideshare1_user";
    private static final String DEFAULT_PASSWORD = "hqhmQ01YswRr4Z1Oyk7A0cha44DkT9cC";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            
            // Check for DATABASE_URL environment variable (Render/Heroku format)
            String databaseUrl = System.getenv("DATABASE_URL");
            
            if (databaseUrl != null && !databaseUrl.isEmpty()) {
                // Parse DATABASE_URL format: postgresql://user:password@host:port/database
                return getConnectionFromDatabaseUrl(databaseUrl);
            } else {
                // Use default configuration for local development
                return DriverManager.getConnection(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found", e);
        }
    }
    
    private static Connection getConnectionFromDatabaseUrl(String databaseUrl) throws SQLException {
        try {
            URI dbUri = new URI(databaseUrl);
            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String jdbcUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
            
            return DriverManager.getConnection(jdbcUrl, username, password);
        } catch (URISyntaxException e) {
            throw new SQLException("Invalid DATABASE_URL format", e);
        }
    }
}
