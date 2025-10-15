package com.rideshare.domain.unitofwork;

import com.rideshare.config.DatabaseConfig;
import com.rideshare.util.RideShareException;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseUnitOfWork implements UnitOfWork {
    private Connection connection;

    @Override
    public void begin() {
        try {
            connection = DatabaseConfig.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new RideShareException("Failed to begin transaction", e);
        }
    }

    @Override
    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new RideShareException("Failed to commit transaction", e);
        } finally {
            closeConnection();
        }
    }

    @Override
    public void rollback() {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to rollback transaction", e);
        } finally {
            closeConnection();
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    private void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to close connection", e);
        }
    }
}
