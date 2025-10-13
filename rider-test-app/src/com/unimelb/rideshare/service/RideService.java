package com.unimelb.rideshare.service;

import com.unimelb.rideshare.common.Result;
import com.unimelb.rideshare.domain.model.Ride;
import com.unimelb.rideshare.domain.model.RideRequest;
import com.unimelb.rideshare.domain.value.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages the ride lifecycle across request, matching and completion.
 */
public interface RideService {
    Result<RideRequest> requestRide(UUID riderId, Location pickup, Location dropOff);

    Result<Ride> matchRide(UUID rideRequestId);

    Result<Ride> acceptRide(UUID rideId, UUID driverId);

    Result<Ride> startRide(UUID rideId);

    Result<Ride> completeRide(UUID rideId);

    Result<RideRequest> cancelRequest(UUID rideRequestId);

    Optional<Ride> findRide(UUID rideId);

    List<RideRequest> listOpenRequests();

    List<Ride> listActiveRides();
}
