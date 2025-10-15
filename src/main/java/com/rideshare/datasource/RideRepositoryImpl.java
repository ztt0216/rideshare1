package com.rideshare.datasource;

import com.rideshare.domain.Ride;
import com.rideshare.domain.RideRepository;
import com.rideshare.domain.RideStatus;
import com.rideshare.util.RideShareException;
import com.rideshare.util.TimeZoneUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RideRepositoryImpl implements RideRepository {
    private Connection connection;

    public RideRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Long insert(Ride ride) {
        String sql = "INSERT INTO rides (rider_id, driver_id, pickup_location, destination, fare, status, requested_time, completed_time, version) " +
                     "VALUES (?, ?, ?, ?, ?, ?::ride_status, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, ride.getRiderId());
            if (ride.getDriverId() != null) {
                stmt.setLong(2, ride.getDriverId());
            } else {
                stmt.setNull(2, Types.BIGINT);
            }
            stmt.setString(3, ride.getPickupLocation());
            stmt.setString(4, ride.getDestination());
            stmt.setBigDecimal(5, ride.getFare());
            stmt.setString(6, ride.getStatus().name());
            stmt.setTimestamp(7, Timestamp.valueOf(ride.getRequestedTime().atZone(TimeZoneUtil.MELBOURNE_ZONE).toLocalDateTime()));
            if (ride.getCompletedTime() != null) {
                stmt.setTimestamp(8, Timestamp.valueOf(ride.getCompletedTime().atZone(TimeZoneUtil.MELBOURNE_ZONE).toLocalDateTime()));
            } else {
                stmt.setNull(8, Types.TIMESTAMP);
            }
            stmt.setInt(9, ride.getVersion());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    ride.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to insert ride", e);
        }
        throw new RideShareException("Failed to insert ride: no ID returned");
    }

    @Override
    public Ride findById(Long id) {
        String sql = "SELECT * FROM rides WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToRide(rs);
                }
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to find ride by id", e);
        }
        return null;
    }

    @Override
    public List<Ride> findByRiderId(Long riderId) {
        String sql = "SELECT * FROM rides WHERE rider_id = ? ORDER BY requested_time DESC";
        List<Ride> rides = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, riderId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rides.add(mapResultSetToRide(rs));
                }
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to find rides by rider id", e);
        }
        return rides;
    }

    @Override
    public List<Ride> findByDriverId(Long driverId) {
        String sql = "SELECT * FROM rides WHERE driver_id = ? ORDER BY requested_time DESC";
        List<Ride> rides = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, driverId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rides.add(mapResultSetToRide(rs));
                }
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to find rides by driver id", e);
        }
        return rides;
    }

    @Override
    public List<Ride> findAvailableRides() {
        String sql = "SELECT * FROM rides WHERE status = 'REQUESTED' ORDER BY requested_time ASC";
        List<Ride> rides = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rides.add(mapResultSetToRide(rs));
                }
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to find available rides", e);
        }
        return rides;
    }

    @Override
    public void update(Ride ride) {
        String sql = "UPDATE rides SET driver_id = ?, status = ?::ride_status, completed_time = ?, version = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (ride.getDriverId() != null) {
                stmt.setLong(1, ride.getDriverId());
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            stmt.setString(2, ride.getStatus().name());
            if (ride.getCompletedTime() != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(ride.getCompletedTime().atZone(TimeZoneUtil.MELBOURNE_ZONE).toLocalDateTime()));
            } else {
                stmt.setNull(3, Types.TIMESTAMP);
            }
            stmt.setInt(4, ride.getVersion());
            stmt.setLong(5, ride.getId());
            
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RideShareException("Failed to update ride", e);
        }
    }

    @Override
    public void updateWithVersion(Ride ride) {
        String sql = "UPDATE rides SET driver_id = ?, status = ?::ride_status, completed_time = ?, version = ? WHERE id = ? AND version = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int oldVersion = ride.getVersion();
            ride.setVersion(oldVersion + 1);
            
            if (ride.getDriverId() != null) {
                stmt.setLong(1, ride.getDriverId());
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            stmt.setString(2, ride.getStatus().name());
            if (ride.getCompletedTime() != null) {
                stmt.setTimestamp(3, Timestamp.valueOf(ride.getCompletedTime().atZone(TimeZoneUtil.MELBOURNE_ZONE).toLocalDateTime()));
            } else {
                stmt.setNull(3, Types.TIMESTAMP);
            }
            stmt.setInt(4, ride.getVersion());
            stmt.setLong(5, ride.getId());
            stmt.setInt(6, oldVersion);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RideShareException("Optimistic lock failure: ride has been modified by another transaction");
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to update ride with version check", e);
        }
    }

    private Ride mapResultSetToRide(ResultSet rs) throws SQLException {
        Ride ride = new Ride(
            rs.getLong("rider_id"),
            rs.getString("pickup_location"),
            rs.getString("destination")
        );
        
        ride.setId(rs.getLong("id"));
        
        Long driverId = rs.getLong("driver_id");
        if (!rs.wasNull()) {
            ride.setDriverId(driverId);
        }
        
        ride.setFare(rs.getBigDecimal("fare"));
        ride.setStatus(RideStatus.valueOf(rs.getString("status")));
        
        Timestamp requestedTime = rs.getTimestamp("requested_time");
        ride.setRequestedTime(requestedTime.toLocalDateTime().atZone(TimeZoneUtil.MELBOURNE_ZONE).toInstant());
        
        Timestamp completedTime = rs.getTimestamp("completed_time");
        if (completedTime != null) {
            ride.setCompletedTime(completedTime.toLocalDateTime().atZone(TimeZoneUtil.MELBOURNE_ZONE).toInstant());
        }
        
        ride.setVersion(rs.getInt("version"));
        
        return ride;
    }
}
