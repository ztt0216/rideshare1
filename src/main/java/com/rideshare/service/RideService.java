package com.rideshare.service;

import com.rideshare.domain.Ride;

import java.util.List;

public interface RideService {
    // Rider operations
    Long requestRide(Long riderId, String pickupLocation, String destination);
    void cancelRide(Long rideId, Long riderId);
    List<Ride> getRiderHistory(Long riderId);
    
    // Driver operations
    List<Ride> getAvailableRides();
    void acceptRide(Long rideId, Long driverId);
    void startRide(Long rideId, Long driverId);
    void completeRide(Long rideId, Long driverId);
    List<Ride> getDriverHistory(Long driverId);
    
    // Common
    Ride getRideById(Long rideId);
}
