package com.rideshare.util;

import com.rideshare.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Drop tables if exist (in reverse order due to foreign keys)
            stmt.execute("DROP TABLE IF EXISTS payments CASCADE");
            System.out.println("Dropped existing payments table (if any)");
            
            stmt.execute("DROP TABLE IF EXISTS rides CASCADE");
            System.out.println("Dropped existing rides table (if any)");
            
            stmt.execute("DROP TABLE IF EXISTS driver_availability CASCADE");
            System.out.println("Dropped existing driver_availability table (if any)");
            
            stmt.execute("DROP TABLE IF EXISTS users CASCADE");
            System.out.println("Dropped existing users table (if any)");
            
            stmt.execute("DROP TYPE IF EXISTS ride_status CASCADE");
            System.out.println("Dropped existing ride_status type (if any)");
            
            // Create ride_status enum type
            String createRideStatusType = "CREATE TYPE ride_status AS ENUM ('REQUESTED', 'ACCEPTED', 'ENROUTE', 'COMPLETED', 'CANCELLED')";
            stmt.execute(createRideStatusType);
            System.out.println("Created ride_status type successfully!");
            
            // Create users table
            String createUsersTable = "CREATE TABLE users (" +
                    "id SERIAL PRIMARY KEY," +
                    "name VARCHAR(255) NOT NULL," +
                    "email VARCHAR(255) NOT NULL UNIQUE," +
                    "password VARCHAR(255) NOT NULL," +
                    "role VARCHAR(50) NOT NULL," +
                    "wallet_balance DECIMAL(10,2) NOT NULL DEFAULT 0.00" +
                    ")";
            stmt.execute(createUsersTable);
            System.out.println("Created users table successfully!");
            
            // Create driver_availability table
            String createAvailabilityTable = "CREATE TABLE driver_availability (" +
                    "id SERIAL PRIMARY KEY," +
                    "driver_id BIGINT NOT NULL," +
                    "day_of_week VARCHAR(20) NOT NULL," +
                    "start_time TIME NOT NULL," +
                    "end_time TIME NOT NULL," +
                    "FOREIGN KEY (driver_id) REFERENCES users(id) ON DELETE CASCADE" +
                    ")";
            stmt.execute(createAvailabilityTable);
            System.out.println("Created driver_availability table successfully!");
            
            // Create rides table
            String createRidesTable = "CREATE TABLE rides (" +
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
                    ")";
            stmt.execute(createRidesTable);
            System.out.println("Created rides table successfully!");
            
            // Create payments table
            String createPaymentsTable = "CREATE TABLE payments (" +
                    "id SERIAL PRIMARY KEY," +
                    "ride_id BIGINT NOT NULL UNIQUE," +
                    "amount DECIMAL(10,2) NOT NULL," +
                    "payment_time TIMESTAMP NOT NULL," +
                    "FOREIGN KEY (ride_id) REFERENCES rides(id)" +
                    ")";
            stmt.execute(createPaymentsTable);
            System.out.println("Created payments table successfully!");
            
            System.out.println("\nAll tables created successfully!");
            
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
