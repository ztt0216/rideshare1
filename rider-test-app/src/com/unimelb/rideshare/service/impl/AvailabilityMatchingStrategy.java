package com.unimelb.rideshare.service.impl;

import com.unimelb.rideshare.concurrency.DriverAvailabilityGuard;
import com.unimelb.rideshare.domain.model.Driver;
import com.unimelb.rideshare.domain.model.RideRequest;
import com.unimelb.rideshare.domain.value.AvailabilitySchedule;
import com.unimelb.rideshare.mapper.DriverDataMapper;
import com.unimelb.rideshare.service.MatchingStrategy;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Very simple matching strategy that checks weekly availability windows.
 */
public final class AvailabilityMatchingStrategy implements MatchingStrategy {
    private final DriverDataMapper driverMapper;
    private final DriverAvailabilityGuard availabilityGuard;

    public AvailabilityMatchingStrategy(DriverDataMapper driverMapper,
                                        DriverAvailabilityGuard availabilityGuard) {
        this.driverMapper = driverMapper;
        this.availabilityGuard = availabilityGuard;
    }

    @Override
    public Optional<Driver> selectDriver(RideRequest request) {
        DayOfWeek day = request.getRequestedAt().getDayOfWeek();
        LocalTime time = request.getRequestedAt().toLocalTime();
        List<Driver> drivers = driverMapper.findAll();
        return drivers.stream()
                .filter(driver -> availabilityGuard.withReadLock(
                        driver.getId(),
                        () -> isAvailable(driver.getAvailabilitySchedule(), day, time)))
                .findFirst();
    }

    private boolean isAvailable(AvailabilitySchedule schedule, DayOfWeek day, LocalTime time) {
        return schedule != null && schedule.isAvailable(day, time);
    }
}