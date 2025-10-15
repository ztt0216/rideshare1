package com.rideshare.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class Ride {
    private Long id;
    private Long riderId;
    private Long driverId;
    private String pickupLocation;
    private String destination;
    private BigDecimal fare;
    private RideStatus status;
    private Instant requestedTime;
    private Instant completedTime;
    private Integer version; // For optimistic locking

    // Constructor for new ride (used by service layer)
    public Ride(Long riderId, String pickupLocation, String destination) {
        this.riderId = riderId;
        this.pickupLocation = pickupLocation;
        this.destination = destination;
        this.status = RideStatus.REQUESTED;
        this.requestedTime = Instant.now();
        this.version = 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRiderId() {
        return riderId;
    }

    public void setRiderId(Long riderId) {
        this.riderId = riderId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public BigDecimal getFare() {
        return fare;
    }

    public void setFare(BigDecimal fare) {
        this.fare = fare;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public Instant getRequestedTime() {
        return requestedTime;
    }

    public void setRequestedTime(Instant requestedTime) {
        this.requestedTime = requestedTime;
    }

    public Instant getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(Instant completedTime) {
        this.completedTime = completedTime;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    // Business methods for state transitions
    public boolean canBeAccepted() {
        return this.status == RideStatus.REQUESTED;
    }

    public boolean canBeStarted() {
        return this.status == RideStatus.ACCEPTED;
    }

    public boolean canBeCompleted() {
        return this.status == RideStatus.ENROUTE;
    }

    public boolean canBeCancelled() {
        return this.status == RideStatus.REQUESTED;
    }
}
