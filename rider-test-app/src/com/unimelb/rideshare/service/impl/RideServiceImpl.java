package com.unimelb.rideshare.service.impl;

import com.unimelb.rideshare.common.Result;
import com.unimelb.rideshare.concurrency.LockManager;
import com.unimelb.rideshare.domain.model.Driver;
import com.unimelb.rideshare.domain.model.Ride;
import com.unimelb.rideshare.domain.model.RideRequest;
import com.unimelb.rideshare.domain.model.RideStatus;
import com.unimelb.rideshare.domain.model.Rider;
import com.unimelb.rideshare.domain.value.Location;
import com.unimelb.rideshare.mapper.DriverDataMapper;
import com.unimelb.rideshare.mapper.RideDataMapper;
import com.unimelb.rideshare.mapper.RideRequestDataMapper;
import com.unimelb.rideshare.mapper.RiderDataMapper;
import com.unimelb.rideshare.service.MatchingStrategy;
import com.unimelb.rideshare.service.NotificationService;
import com.unimelb.rideshare.service.RideService;
import com.unimelb.rideshare.unitofwork.UnitOfWork;
import com.unimelb.rideshare.unitofwork.UnitOfWorkFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * Coordinates the ride lifecycle across mappers and services.
 */
public final class RideServiceImpl implements RideService {
    private final RiderDataMapper riderMapper;
    private final DriverDataMapper driverMapper;
    private final RideRequestDataMapper rideRequestMapper;
    private final RideDataMapper rideMapper;
    private final MatchingStrategy matchingStrategy;
    private final NotificationService notificationService;
    private final UnitOfWorkFactory unitOfWorkFactory;
    private final LockManager lockManager;

    public RideServiceImpl(RiderDataMapper riderMapper,
                           DriverDataMapper driverMapper,
                           RideRequestDataMapper rideRequestMapper,
                           RideDataMapper rideMapper,
                           MatchingStrategy matchingStrategy,
                           NotificationService notificationService,
                           UnitOfWorkFactory unitOfWorkFactory,
                           LockManager lockManager) {
        this.riderMapper = riderMapper;
        this.driverMapper = driverMapper;
        this.rideRequestMapper = rideRequestMapper;
        this.rideMapper = rideMapper;
        this.matchingStrategy = matchingStrategy;
        this.notificationService = notificationService;
        this.unitOfWorkFactory = unitOfWorkFactory;
        this.lockManager = lockManager;
    }

    @Override
    public Result<RideRequest> requestRide(UUID riderId, Location pickup, Location dropOff) {
        Optional<Rider> riderOpt = riderMapper.findById(riderId);
        if (riderOpt.isEmpty()) {
            return Result.fail("Rider not found");
        }
        try {
            RideRequest rideRequest = new RideRequest(UUID.randomUUID(), riderId, pickup, dropOff, LocalDateTime.now());
            UnitOfWork unitOfWork = unitOfWorkFactory.create();
            unitOfWork.registerNew(rideRequest, RideRequest.class);
            unitOfWork.commit();
            notificationService.notifyRider(riderOpt.get().getEmail(), "Ride request submitted");
            return Result.ok(rideRequest);
        } catch (IllegalArgumentException ex) {
            return Result.fail(ex.getMessage());
        }
    }

    @Override
    public Result<Ride> matchRide(UUID rideRequestId) {
        Optional<RideRequest> requestOpt = rideRequestMapper.findById(rideRequestId);
        if (requestOpt.isEmpty()) {
            return Result.fail("Ride request not found");
        }
        RideRequest request = requestOpt.get();
        if (request.getStatus() != RideStatus.REQUESTED) {
            return Result.fail("Ride request not in a matchable state");
        }
        Optional<Driver> driverOpt = matchingStrategy.selectDriver(request);
        if (driverOpt.isEmpty()) {
            return Result.fail("No drivers available");
        }
        Driver driver = driverOpt.get();
        request.assignDriver(driver.getId());
        Ride ride = new Ride(UUID.randomUUID(), request.getId(), driver.getId(), request.getRiderId(),
                request.getPickup(), request.getDropOff(), request.getRequestedAt());
        UnitOfWork unitOfWork = unitOfWorkFactory.create();
        unitOfWork.registerDirty(request, RideRequest.class);
        unitOfWork.registerNew(ride, Ride.class);
        unitOfWork.commit();
        notificationService.notifyDriver(driver.getEmail(), "New ride assigned");
        return Result.ok(ride);
    }

    @Override
    public Result<Ride> acceptRide(UUID rideId, UUID driverId) {
        Optional<Ride> rideOpt = rideMapper.findById(rideId);
        if (rideOpt.isEmpty()) {
            return Result.fail("Ride not found");
        }
        Ride ride = rideOpt.get();
        if (!ride.getDriverId().equals(driverId)) {
            return Result.fail("Driver not assigned to this ride");
        }
        try {
            if (!lockManager.tryAcquire(rideId, 5, TimeUnit.SECONDS)) {
                return Result.fail("Ride is locked by another operation");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.fail("Interrupted while waiting for ride lock");
        }
        try {
            if (ride.getStatus() != RideStatus.MATCHED) {
                return Result.fail("Ride already accepted or completed");
            }
            ride.markAccepted();
            Optional<RideRequest> requestOpt = rideRequestMapper.findById(ride.getRideRequestId());
            requestOpt.ifPresent(RideRequest::markAccepted);
            UnitOfWork unitOfWork = unitOfWorkFactory.create();
            unitOfWork.registerDirty(ride, Ride.class);
            requestOpt.ifPresent(req -> unitOfWork.registerDirty(req, RideRequest.class));
            unitOfWork.commit();
            driverMapper.findById(driverId)
                    .ifPresent(driver -> notificationService.notifyDriver(driver.getEmail(), "Ride accepted"));
            return Result.ok(ride);
        } finally {
            lockManager.release(rideId);
        }
    }

    @Override
    public Result<Ride> startRide(UUID rideId) {
        return transitionRide(rideId, RideStatus.ACCEPTED, RideStatus.IN_PROGRESS, Ride::markStarted);
    }

    @Override
    public Result<Ride> completeRide(UUID rideId) {
        return transitionRide(rideId, RideStatus.IN_PROGRESS, RideStatus.COMPLETED, Ride::markCompleted);
    }

    @Override
    public Result<RideRequest> cancelRequest(UUID rideRequestId) {
        Optional<RideRequest> requestOpt = rideRequestMapper.findById(rideRequestId);
        if (requestOpt.isEmpty()) {
            return Result.fail("Ride request not found");
        }
        RideRequest request = requestOpt.get();
        if (request.getStatus() == RideStatus.COMPLETED) {
            return Result.fail("Completed rides cannot be cancelled");
        }
        request.cancel();
        UnitOfWork unitOfWork = unitOfWorkFactory.create();
        unitOfWork.registerDirty(request, RideRequest.class);
        unitOfWork.commit();
        return Result.ok(request);
    }

    @Override
    public Optional<Ride> findRide(UUID rideId) {
        return rideMapper.findById(rideId);
    }

    @Override
    public List<RideRequest> listOpenRequests() {
        return rideRequestMapper.findAll().stream()
                .filter(req -> req.getStatus() == RideStatus.REQUESTED || req.getStatus() == RideStatus.MATCHED)
                .collect(Collectors.toList());
    }

    @Override
    public List<Ride> listActiveRides() {
        return rideMapper.findAll().stream()
                .filter(ride -> ride.getStatus() != RideStatus.COMPLETED && ride.getStatus() != RideStatus.CANCELLED)
                .collect(Collectors.toList());
    }

    private Result<Ride> transitionRide(UUID rideId,
                                        RideStatus requiredState,
                                        RideStatus resultingState,
                                        java.util.function.Consumer<Ride> transition) {
        Optional<Ride> rideOpt = rideMapper.findById(rideId);
        if (rideOpt.isEmpty()) {
            return Result.fail("Ride not found");
        }
        Ride ride = rideOpt.get();
        if (ride.getStatus() != requiredState) {
            return Result.fail("Ride must be in state " + requiredState + " to transition");
        }
        transition.accept(ride);
        Optional<RideRequest> requestOpt = rideRequestMapper.findById(ride.getRideRequestId());
        requestOpt.ifPresent(req -> {
            if (resultingState == RideStatus.IN_PROGRESS) {
                req.markInProgress();
            } else if (resultingState == RideStatus.COMPLETED) {
                req.markCompleted();
            }
        });
        UnitOfWork unitOfWork = unitOfWorkFactory.create();
        unitOfWork.registerDirty(ride, Ride.class);
        requestOpt.ifPresent(req -> unitOfWork.registerDirty(req, RideRequest.class));
        unitOfWork.commit();
        return Result.ok(ride);
    }
}
