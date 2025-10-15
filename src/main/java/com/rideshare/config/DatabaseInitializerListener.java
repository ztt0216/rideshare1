package com.rideshare.config;

import com.rideshare.util.DatabaseInitializer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Connection;
import java.sql.Statement;

@WebListener
public class DatabaseInitializerListener implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("========================================");
        System.out.println("Starting database initialization...");
        System.out.println("========================================");
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Check if tables exist
            boolean tablesExist = false;
            try {
                stmt.executeQuery("SELECT 1 FROM users LIMIT 1");
                stmt.executeQuery("SELECT 1 FROM rides LIMIT 1");
                tablesExist = true;
                System.out.println("✓ Database tables already exist");
            } catch (Exception e) {
                System.out.println("✗ Database tables do not exist, creating...");
            }
            
            if (!tablesExist) {
                // Run DatabaseInitializer
                initializeTables(conn, stmt);
            }
            
            System.out.println("========================================");
            System.out.println("Database initialization completed!");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("FATAL: Failed to initialize database!");
            System.err.println("========================================");
            e.printStackTrace();
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    private void initializeTables(Connection conn, Statement stmt) throws Exception {
        // Drop existing objects if any
        stmt.execute("DROP TABLE IF EXISTS payments CASCADE");
        System.out.println("  - Dropped existing payments table (if any)");
        
        stmt.execute("DROP TABLE IF EXISTS rides CASCADE");
        System.out.println("  - Dropped existing rides table (if any)");
        
        stmt.execute("DROP TABLE IF EXISTS driver_availability CASCADE");
        System.out.println("  - Dropped existing driver_availability table (if any)");
        
        stmt.execute("DROP TABLE IF EXISTS users CASCADE");
        System.out.println("  - Dropped existing users table (if any)");
        
        stmt.execute("DROP TYPE IF EXISTS ride_status CASCADE");
        System.out.println("  - Dropped existing ride_status type (if any)");
        
        // Create ride_status enum type
        stmt.execute("CREATE TYPE ride_status AS ENUM ('REQUESTED', 'ACCEPTED', 'ENROUTE', 'COMPLETED', 'CANCELLED')");
        System.out.println("  ✓ Created ride_status type");
        
        // Create users table
        stmt.execute("CREATE TABLE users (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(255) NOT NULL," +
                "email VARCHAR(255) NOT NULL UNIQUE," +
                "password VARCHAR(255) NOT NULL," +
                "role VARCHAR(50) NOT NULL," +
                "wallet_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00" +
                ")");
        System.out.println("  ✓ Created users table");
        
        // Create driver_availability table
        stmt.execute("CREATE TABLE driver_availability (" +
                "id SERIAL PRIMARY KEY," +
                "driver_id BIGINT NOT NULL," +
                "day_of_week VARCHAR(20) NOT NULL," +
                "start_time TIME NOT NULL," +
                "end_time TIME NOT NULL," +
                "FOREIGN KEY (driver_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");
        System.out.println("  ✓ Created driver_availability table");
        
        // Create rides table
        stmt.execute("CREATE TABLE rides (" +
                "id SERIAL PRIMARY KEY," +
                "rider_id BIGINT NOT NULL," +
                "driver_id BIGINT," +
                "pickup_location VARCHAR(500) NOT NULL," +
                "destination VARCHAR(500) NOT NULL," +
                "fare DECIMAL(10,2) NOT NULL," +
                "status ride_status NOT NULL DEFAULT 'REQUESTED'," +
                "requested_time TIMESTAMP NOT NULL," +
                "completed_time TIMESTAMP," +
                "version INTEGER NOT NULL DEFAULT 0," +
                "FOREIGN KEY (rider_id) REFERENCES users(id)," +
                "FOREIGN KEY (driver_id) REFERENCES users(id)" +
                ")");
        System.out.println("  ✓ Created rides table");
        
        // Create indexes for rides
        stmt.execute("CREATE INDEX idx_rides_rider_id ON rides(rider_id)");
        stmt.execute("CREATE INDEX idx_rides_driver_id ON rides(driver_id)");
        stmt.execute("CREATE INDEX idx_rides_status ON rides(status)");
        System.out.println("  ✓ Created indexes for rides table");
        
        // Create payments table
        stmt.execute("CREATE TABLE payments (" +
                "id SERIAL PRIMARY KEY," +
                "ride_id BIGINT NOT NULL UNIQUE," +
                "amount DECIMAL(10,2) NOT NULL," +
                "payment_time TIMESTAMP NOT NULL," +
                "FOREIGN KEY (ride_id) REFERENCES rides(id)" +
                ")");
        System.out.println("  ✓ Created payments table");
        
        System.out.println("  ✓ All tables created successfully!");
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Application shutting down...");
    }
}
