package com.rideshare.service;

import com.rideshare.domain.*;
import com.rideshare.domain.unitofwork.UnitOfWork;
import com.rideshare.datasource.RideRepositoryImpl;
import com.rideshare.datasource.PaymentRepositoryImpl;
import com.rideshare.datasource.UserRepositoryImpl;
import com.rideshare.util.RideShareException;
import com.rideshare.util.TimeZoneUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

public class RideServiceImpl implements RideService {
    
    private UnitOfWork unitOfWork;
    private FareCalculationService fareCalculationService;

    public RideServiceImpl(UnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
        this.fareCalculationService = new FareCalculationServiceImpl();
    }

    @Override
    public Long requestRide(Long riderId, String pickupLocation, String destination) {
        try {
            unitOfWork.begin();
            Connection conn = unitOfWork.getConnection();
            
            // Verify rider exists
            UserRepository userRepo = new UserRepositoryImpl();
            User rider = userRepo.findById(riderId, conn)
                .orElseThrow(() -> new RideShareException("Rider not found"));
            
            // Calculate fare
            BigDecimal fare = fareCalculationService.calculateFare(destination);
            
            // Check if rider has sufficient balance
            if (rider.getWalletBalance().compareTo(fare) < 0) {
                throw new RideShareException("Insufficient wallet balance. Required: $" + fare + ", Available: $" + rider.getWalletBalance());
            }
            
            // Create ride
            Ride ride = new Ride(riderId, pickupLocation, destination);
            ride.setFare(fare);
            
            RideRepository rideRepo = new RideRepositoryImpl(conn);
            Long rideId = rideRepo.insert(ride);
            
            unitOfWork.commit();
            return rideId;
        } catch (Exception e) {
            unitOfWork.rollback();
            if (e instanceof RideShareException) {
                throw (RideShareException) e;
            }
            throw new RideShareException("Failed to request ride", e);
        }
    }

    @Override
    public void cancelRide(Long rideId, Long riderId) {
        try {
            unitOfWork.begin();
            Connection conn = unitOfWork.getConnection();
            
            RideRepository rideRepo = new RideRepositoryImpl(conn);
            Ride ride = rideRepo.findById(rideId);
            
            if (ride == null) {
                throw new RideShareException("Ride not found");
            }
            
            if (!ride.getRiderId().equals(riderId)) {
                throw new RideShareException("Unauthorized: not your ride");
            }
            
            if (!ride.canBeCancelled()) {
                throw new RideShareException("Ride cannot be cancelled in current status: " + ride.getStatus());
            }
            
            ride.setStatus(RideStatus.CANCELLED);
            rideRepo.update(ride);
            
            unitOfWork.commit();
        } catch (Exception e) {
            unitOfWork.rollback();
            if (e instanceof RideShareException) {
                throw (RideShareException) e;
            }
            throw new RideShareException("Failed to cancel ride", e);
        }
    }

    @Override
    public List<Ride> getRiderHistory(Long riderId) {
        try {
            unitOfWork.begin();
            Connection conn = unitOfWork.getConnection();
            
            RideRepository rideRepo = new RideRepositoryImpl(conn);
            List<Ride> rides = rideRepo.findByRiderId(riderId);
            
            unitOfWork.commit();
            return rides;
        } catch (Exception e) {
            unitOfWork.rollback();
            throw new RideShareException("Failed to get rider history", e);
        }
    }

    @Override
    public List<Ride> getAvailableRides() {
        try {
            unitOfWork.begin();
            Connection conn = unitOfWork.getConnection();
            
            RideRepository rideRepo = new RideRepositoryImpl(conn);
            List<Ride> rides = rideRepo.findAvailableRides();
            
            unitOfWork.commit();
            return rides;
        } catch (Exception e) {
            unitOfWork.rollback();
            throw new RideShareException("Failed to get available rides", e);
        }
    }

    @Override
    public void acceptRide(Long rideId, Long driverId) {
        try {
            unitOfWork.begin();
            Connection conn = unitOfWork.getConnection();
            
            // Verify driver exists and is a driver
            UserRepository userRepo = new UserRepositoryImpl();
            User driver = userRepo.findById(driverId, conn)
                .orElseThrow(() -> new RideShareException("Driver not found"));
            if (driver.getRole() != UserRole.DRIVER) {
                throw new RideShareException("User is not a driver");
            }
            
            // Get ride with optimistic locking
            RideRepository rideRepo = new RideRepositoryImpl(conn);
            Ride ride = rideRepo.findById(rideId);
            
            if (ride == null) {
                throw new RideShareException("Ride not found");
            }
            
            if (!ride.canBeAccepted()) {
                throw new RideShareException("Ride cannot be accepted in current status: " + ride.getStatus());
            }
            
            // Update ride with version check (optimistic locking)
            ride.setDriverId(driverId);
            ride.setStatus(RideStatus.ACCEPTED);
            rideRepo.updateWithVersion(ride);
            
            unitOfWork.commit();
        } catch (Exception e) {
            unitOfWork.rollback();
            if (e instanceof RideShareException) {
                throw (RideShareException) e;
            }
            throw new RideShareException("Failed to accept ride", e);
        }
    }

    @Override
    public void startRide(Long rideId, Long driverId) {
        try {
            unitOfWork.begin();
            Connection conn = unitOfWork.getConnection();
            
            RideRepository rideRepo = new RideRepositoryImpl(conn);
            Ride ride = rideRepo.findById(rideId);
            
            if (ride == null) {
                throw new RideShareException("Ride not found");
            }
            
            if (!ride.getDriverId().equals(driverId)) {
                throw new RideShareException("Unauthorized: not your ride");
            }
            
            if (!ride.canBeStarted()) {
                throw new RideShareException("Ride cannot be started in current status: " + ride.getStatus());
            }
            
            ride.setStatus(RideStatus.ENROUTE);
            rideRepo.update(ride);
            
            unitOfWork.commit();
        } catch (Exception e) {
            unitOfWork.rollback();
            if (e instanceof RideShareException) {
                throw (RideShareException) e;
            }
            throw new RideShareException("Failed to start ride", e);
        }
    }

    @Override
    public void completeRide(Long rideId, Long driverId) {
        try {
            unitOfWork.begin();
            Connection conn = unitOfWork.getConnection();
            
            RideRepository rideRepo = new RideRepositoryImpl(conn);
            Ride ride = rideRepo.findById(rideId);
            
            if (ride == null) {
                throw new RideShareException("Ride not found");
            }
            
            if (!ride.getDriverId().equals(driverId)) {
                throw new RideShareException("Unauthorized: not your ride");
            }
            
            if (!ride.canBeCompleted()) {
                throw new RideShareException("Ride cannot be completed in current status: " + ride.getStatus());
            }
            
            // Process payment with pessimistic locking on wallet operations
            UserRepository userRepo = new UserRepositoryImpl();
            
            // Lock and update rider wallet (deduct fare)
            User rider = userRepo.findById(ride.getRiderId(), conn)
                .orElseThrow(() -> new RideShareException("Rider not found"));
            if (rider.getWalletBalance().compareTo(ride.getFare()) < 0) {
                throw new RideShareException("Insufficient wallet balance");
            }
            rider.setWalletBalance(rider.getWalletBalance().subtract(ride.getFare()));
            userRepo.update(rider, conn);
            
            // Lock and update driver wallet (credit fare)
            User driver = userRepo.findById(driverId, conn)
                .orElseThrow(() -> new RideShareException("Driver not found"));
            driver.setWalletBalance(driver.getWalletBalance().add(ride.getFare()));
            userRepo.update(driver, conn);
            
            // Create payment record
            Payment payment = new Payment(rideId, ride.getFare());
            PaymentRepository paymentRepo = new PaymentRepositoryImpl(conn);
            paymentRepo.insert(payment);
            
            // Update ride status
            ride.setStatus(RideStatus.COMPLETED);
            ride.setCompletedTime(Instant.now());
            rideRepo.update(ride);
            
            unitOfWork.commit();
        } catch (Exception e) {
            unitOfWork.rollback();
            if (e instanceof RideShareException) {
                throw (RideShareException) e;
            }
            throw new RideShareException("Failed to complete ride", e);
        }
    }

    @Override
    public List<Ride> getDriverHistory(Long driverId) {
        try {
            unitOfWork.begin();
            Connection conn = unitOfWork.getConnection();
            
            RideRepository rideRepo = new RideRepositoryImpl(conn);
            List<Ride> rides = rideRepo.findByDriverId(driverId);
            
            unitOfWork.commit();
            return rides;
        } catch (Exception e) {
            unitOfWork.rollback();
            throw new RideShareException("Failed to get driver history", e);
        }
    }

    @Override
    public Ride getRideById(Long rideId) {
        try {
            unitOfWork.begin();
            Connection conn = unitOfWork.getConnection();
            
            RideRepository rideRepo = new RideRepositoryImpl(conn);
            Ride ride = rideRepo.findById(rideId);
            
            unitOfWork.commit();
            return ride;
        } catch (Exception e) {
            unitOfWork.rollback();
            throw new RideShareException("Failed to get ride", e);
        }
    }
}
