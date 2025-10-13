package com.unimelb.rideshare.service;

import com.unimelb.rideshare.common.Result;
import com.unimelb.rideshare.domain.model.Driver;
import com.unimelb.rideshare.domain.value.AvailabilityWindow;
import com.unimelb.rideshare.domain.value.Vehicle;

import java.util.List;
import java.util.UUID;

/**
 * Business operations related to drivers.
 */
public interface DriverService {
    Result<Driver> registerDriver(String name, String email, Vehicle vehicle);

    Result<Driver> updateAvailability(UUID driverId, List<AvailabilityWindow> windows);

    List<Driver> listDrivers();
}
