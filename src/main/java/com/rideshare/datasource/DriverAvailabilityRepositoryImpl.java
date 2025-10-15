package com.rideshare.datasource;

import com.rideshare.domain.DayOfWeek;
import com.rideshare.domain.DriverAvailability;
import com.rideshare.domain.DriverAvailabilityRepository;
import com.rideshare.util.RideShareException;

import java.sql.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DriverAvailabilityRepositoryImpl implements DriverAvailabilityRepository {
    
    @Override
    public DriverAvailability save(DriverAvailability availability, Connection connection) {
        String sql = "INSERT INTO driver_availability (driver_id, day_of_week, start_time, end_time) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, availability.getDriverId());
            ps.setString(2, availability.getDayOfWeek().name());
            ps.setTime(3, Time.valueOf(availability.getStartTime()));
            ps.setTime(4, Time.valueOf(availability.getEndTime()));
            
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    availability.setId(rs.getLong(1));
                    return availability;
                }
                throw new RideShareException("Failed to retrieve availability ID");
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to save driver availability", e);
        }
    }
    
    @Override
    public List<DriverAvailability> findByDriverId(Long driverId, Connection connection) {
        String sql = "SELECT * FROM driver_availability WHERE driver_id = ? ORDER BY day_of_week, start_time";
        List<DriverAvailability> availabilities = new ArrayList<>();
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    availabilities.add(mapResultSetToAvailability(rs));
                }
                return availabilities;
            }
        } catch (SQLException e) {
            throw new RideShareException("Failed to find driver availability", e);
        }
    }
    
    @Override
    public void deleteByDriverId(Long driverId, Connection connection) {
        String sql = "DELETE FROM driver_availability WHERE driver_id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, driverId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RideShareException("Failed to delete driver availability", e);
        }
    }
    
    @Override
    public void delete(Long id, Connection connection) {
        String sql = "DELETE FROM driver_availability WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RideShareException("Failed to delete availability", e);
        }
    }
    
    private DriverAvailability mapResultSetToAvailability(ResultSet rs) throws SQLException {
        DriverAvailability availability = new DriverAvailability(
            rs.getLong("driver_id"),
            DayOfWeek.valueOf(rs.getString("day_of_week")),
            rs.getTime("start_time").toLocalTime(),
            rs.getTime("end_time").toLocalTime()
        );
        availability.setId(rs.getLong("id"));
        return availability;
    }
}
