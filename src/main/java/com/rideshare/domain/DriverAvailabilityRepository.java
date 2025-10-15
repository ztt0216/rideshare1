package com.rideshare.domain;

import java.sql.Connection;
import java.util.List;

public interface DriverAvailabilityRepository {
    DriverAvailability save(DriverAvailability availability, Connection connection);
    List<DriverAvailability> findByDriverId(Long driverId, Connection connection);
    void deleteByDriverId(Long driverId, Connection connection);
    void delete(Long id, Connection connection);
}
