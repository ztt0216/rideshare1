package com.rideshare.service;

import com.rideshare.datasource.DriverAvailabilityRepositoryImpl;
import com.rideshare.datasource.UserRepositoryImpl;
import com.rideshare.domain.*;
import com.rideshare.domain.unitofwork.DatabaseUnitOfWork;
import com.rideshare.domain.unitofwork.UnitOfWork;
import com.rideshare.util.RideShareException;
import com.rideshare.util.TimeZoneUtil;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class DriverAvailabilityServiceImpl implements DriverAvailabilityService {
    private final DriverAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;

    public DriverAvailabilityServiceImpl() {
        this.availabilityRepository = new DriverAvailabilityRepositoryImpl();
        this.userRepository = new UserRepositoryImpl();
    }

    @Override
    public void setAvailability(Long driverId, List<AvailabilitySchedule> schedules) {
        UnitOfWork uow = new DatabaseUnitOfWork();
        try {
            uow.begin();
            
            // Verify user is a driver
            User user = userRepository.findById(driverId, uow.getConnection())
                .orElseThrow(() -> new RideShareException("Driver not found"));
            
            if (user.getRole() != UserRole.DRIVER) {
                throw new RideShareException("User is not a driver");
            }
            
            // Clear existing availability
            availabilityRepository.deleteByDriverId(driverId, uow.getConnection());
            
            // Add new availability schedules
            for (AvailabilitySchedule schedule : schedules) {
                com.rideshare.domain.DayOfWeek dayOfWeek = com.rideshare.domain.DayOfWeek.valueOf(schedule.getDayOfWeek().toUpperCase());
                LocalTime startTime = LocalTime.parse(schedule.getStartTime());
                LocalTime endTime = LocalTime.parse(schedule.getEndTime());
                
                if (startTime.isAfter(endTime)) {
                    throw new RideShareException("Start time must be before end time");
                }
                
                DriverAvailability availability = new DriverAvailability(driverId, dayOfWeek, startTime, endTime);
                availabilityRepository.save(availability, uow.getConnection());
            }
            
            uow.commit();
        } catch (Exception e) {
            uow.rollback();
            throw new RideShareException("Failed to set availability", e);
        }
    }

    @Override
    public List<DriverAvailability> getAvailability(Long driverId) {
        UnitOfWork uow = new DatabaseUnitOfWork();
        try {
            uow.begin();
            
            // Verify user exists
            userRepository.findById(driverId, uow.getConnection())
                .orElseThrow(() -> new RideShareException("Driver not found"));
            
            List<DriverAvailability> availabilities = availabilityRepository.findByDriverId(driverId, uow.getConnection());
            
            uow.commit();
            return availabilities;
        } catch (Exception e) {
            uow.rollback();
            throw new RideShareException("Failed to get availability", e);
        }
    }

    @Override
    public boolean isDriverAvailable(Long driverId) {
        UnitOfWork uow = new DatabaseUnitOfWork();
        try {
            uow.begin();
            
            List<DriverAvailability> availabilities = availabilityRepository.findByDriverId(driverId, uow.getConnection());
            
            uow.commit();
            
            // If no availability configured, driver is available at any time
            if (availabilities.isEmpty()) {
                return true;
            }
            
            // Check if current time (in Melbourne timezone) falls within any availability window
            LocalDateTime now = TimeZoneUtil.nowLocal();
            DayOfWeek currentDay = now.getDayOfWeek();
            LocalTime currentTime = now.toLocalTime();
            
            for (DriverAvailability availability : availabilities) {
                if (availability.getDayOfWeek().name().equals(currentDay.name()) &&
                    !currentTime.isBefore(availability.getStartTime()) &&
                    !currentTime.isAfter(availability.getEndTime())) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            uow.rollback();
            throw new RideShareException("Failed to check availability", e);
        }
    }

    @Override
    public void clearAvailability(Long driverId) {
        UnitOfWork uow = new DatabaseUnitOfWork();
        try {
            uow.begin();
            
            availabilityRepository.deleteByDriverId(driverId, uow.getConnection());
            
            uow.commit();
        } catch (Exception e) {
            uow.rollback();
            throw new RideShareException("Failed to clear availability", e);
        }
    }
}
