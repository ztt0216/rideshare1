package com.unimelb.rideshare.service.impl;

import com.unimelb.rideshare.common.Result;
import com.unimelb.rideshare.concurrency.DriverAvailabilityGuard;
import com.unimelb.rideshare.domain.model.Driver;
import com.unimelb.rideshare.domain.value.AvailabilitySchedule;
import com.unimelb.rideshare.domain.value.AvailabilityWindow;
import com.unimelb.rideshare.domain.value.Vehicle;
import com.unimelb.rideshare.mapper.DriverDataMapper;
import com.unimelb.rideshare.service.DriverService;
import com.unimelb.rideshare.unitofwork.UnitOfWork;
import com.unimelb.rideshare.unitofwork.UnitOfWorkFactory;

import java.util.List;
import java.util.UUID;

/**
 * Default driver service implementation.
 */
public final class DriverServiceImpl implements DriverService {
    private final DriverDataMapper driverMapper;
    private final UnitOfWorkFactory unitOfWorkFactory;
    private final DriverAvailabilityGuard availabilityGuard;

    public DriverServiceImpl(DriverDataMapper driverMapper,
                             UnitOfWorkFactory unitOfWorkFactory,
                             DriverAvailabilityGuard availabilityGuard) {
        this.driverMapper = driverMapper;
        this.unitOfWorkFactory = unitOfWorkFactory;
        this.availabilityGuard = availabilityGuard;
    }

    @Override
    public Result<Driver> registerDriver(String name, String email, Vehicle vehicle) {
        try {
            Driver driver = new Driver(UUID.randomUUID(), name, email, vehicle, new AvailabilitySchedule());
            UnitOfWork unitOfWork = unitOfWorkFactory.create();
            unitOfWork.registerNew(driver, Driver.class);
            unitOfWork.commit();
            availabilityGuard.registerDriver(driver.getId());
            return Result.ok(driver);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        }
    }

    @Override
    public Result<Driver> updateAvailability(UUID driverId, List<AvailabilityWindow> windows) {
        return availabilityGuard.withWriteLock(driverId, () -> driverMapper.findById(driverId)
                .map(driver -> {
                    driver.setAvailabilitySchedule(new AvailabilitySchedule(windows));
                    UnitOfWork unitOfWork = unitOfWorkFactory.create();
                    unitOfWork.registerDirty(driver, Driver.class);
                    unitOfWork.commit();
                    return Result.ok(driver);
                })
                .orElseGet(() -> Result.fail("Driver not found")));
    }

    @Override
    public List<Driver> listDrivers() {
        return driverMapper.findAll();
    }
}