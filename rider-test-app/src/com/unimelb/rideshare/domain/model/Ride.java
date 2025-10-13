package com.unimelb.rideshare.domain.model;

import com.unimelb.rideshare.domain.value.Location;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an active ride pairing between a driver and rider.
 */
public final class Ride extends BaseEntity {
    private final UUID rideRequestId;
    private final UUID driverId;
    private final UUID riderId;
    private final Location pickup;
    private final Location dropOff;
    private final LocalDateTime scheduledStart;
    private LocalDateTime actualStart;
    private LocalDateTime completedAt;
    private RideStatus status;

    public Ride(UUID id, UUID rideRequestId, UUID driverId, UUID riderId, Location pickup, Location dropOff,
                LocalDateTime scheduledStart) {
        super(id);
        this.rideRequestId = rideRequestId;
        this.driverId = driverId;
        this.riderId = riderId;
        this.pickup = pickup;
        this.dropOff = dropOff;
        this.scheduledStart = scheduledStart == null ? LocalDateTime.now() : scheduledStart;
        this.status = RideStatus.MATCHED;
    }

    public UUID getRideRequestId() {
        return rideRequestId;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public UUID getRiderId() {
        return riderId;
    }

    public Location getPickup() {
        return pickup;
    }

    public Location getDropOff() {
        return dropOff;
    }

    public LocalDateTime getScheduledStart() {
        return scheduledStart;
    }

    public LocalDateTime getActualStart() {
        return actualStart;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void markAccepted() {
        status = RideStatus.ACCEPTED;
        touch();
    }

    public void markStarted() {
        status = RideStatus.IN_PROGRESS;
        actualStart = LocalDateTime.now();
        touch();
    }

    public void markCompleted() {
        status = RideStatus.COMPLETED;
        completedAt = LocalDateTime.now();
        touch();
    }

    public void markCancelled() {
        status = RideStatus.CANCELLED;
        touch();
    }

    public void restoreState(RideStatus state, LocalDateTime actualStart, LocalDateTime completedAt) {
        this.status = state;
        this.actualStart = actualStart;
        this.completedAt = completedAt;
    }
}
