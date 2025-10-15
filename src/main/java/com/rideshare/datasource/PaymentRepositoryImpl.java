package com.rideshare.datasource;

import com.rideshare.domain.Payment;
import com.rideshare.domain.PaymentRepository;
import com.rideshare.util.RideShareException;
import com.rideshare.util.TimeZoneUtil;

import java.sql.*;
import java.time.LocalDateTime;

public class PaymentRepositoryImpl implements PaymentRepository {
    private Connection connection;

    public PaymentRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Long insert(Payment payment) {
        String sql = "INSERT INTO payments (ride_id, amount, payment_time) VALUES (?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, payment.getRideId());
            stmt.setBigDecimal(2, payment.getAmount());
            stmt.setTimestamp(3, Timestamp.valueOf(payment.getPaymentTime().atZone(TimeZoneUtil.MELBOURNE_ZONE).toLocalDateTime()));
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    payment.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to insert payment", e);
        }
        throw new RideShareException("Failed to insert payment: no ID returned");
    }

    @Override
    public Payment findById(Long id) {
        String sql = "SELECT * FROM payments WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPayment(rs);
                }
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to find payment by id", e);
        }
        return null;
    }

    @Override
    public Payment findByRideId(Long rideId) {
        String sql = "SELECT * FROM payments WHERE ride_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, rideId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToPayment(rs);
                }
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to find payment by ride id", e);
        }
        return null;
    }

    private Payment mapResultSetToPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment(
            rs.getLong("ride_id"),
            rs.getBigDecimal("amount")
        );
        
        payment.setId(rs.getLong("id"));
        
        Timestamp paymentTime = rs.getTimestamp("payment_time");
        payment.setPaymentTime(paymentTime.toLocalDateTime().atZone(TimeZoneUtil.MELBOURNE_ZONE).toInstant());
        
        return payment;
    }
}
