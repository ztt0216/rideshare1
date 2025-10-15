package com.rideshare.domain;

import java.util.List;

public interface RideRepository {
    Long insert(Ride ride);
    Ride findById(Long id);
    List<Ride> findByRiderId(Long riderId);
    List<Ride> findByDriverId(Long driverId);
    List<Ride> findAvailableRides(); // Status = REQUESTED
    void update(Ride ride);
    void updateWithVersion(Ride ride); // For optimistic locking
}
