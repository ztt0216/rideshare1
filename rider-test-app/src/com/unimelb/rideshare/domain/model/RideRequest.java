package com.unimelb.rideshare.domain.model;

import com.unimelb.rideshare.domain.value.Location;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A rider-initiated request for a trip.
 */
public final class RideRequest extends BaseEntity {
    private final UUID riderId;
    private final Location pickup;
    private final Location dropOff;
    private final LocalDateTime requestedAt;
    private RideStatus status;
    private UUID allocatedDriverId;

    public RideRequest(UUID id, UUID riderId, Location pickup, Location dropOff, LocalDateTime requestedAt) {
        super(id);
        this.riderId = Objects.requireNonNull(riderId, "Rider id cannot be null");
        this.pickup = Objects.requireNonNull(pickup, "Pickup location required");
        this.dropOff = Objects.requireNonNull(dropOff, "Drop off location required");
        this.requestedAt = requestedAt == null ? LocalDateTime.now() : requestedAt;
        this.status = RideStatus.REQUESTED;
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

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public RideStatus getStatus() {
        return status;
    }

    public UUID getAllocatedDriverId() {
        return allocatedDriverId;
    }

    public void assignDriver(UUID driverId) {
        this.allocatedDriverId = Objects.requireNonNull(driverId, "Driver id cannot be null");
        this.status = RideStatus.MATCHED;
        touch();
    }

    public void markAccepted() {
        ensureState(RideStatus.MATCHED);
        this.status = RideStatus.ACCEPTED;
        touch();
    }

    public void markInProgress() {
        ensureState(RideStatus.ACCEPTED);
        this.status = RideStatus.IN_PROGRESS;
        touch();
    }

    public void markCompleted() {
        ensureState(RideStatus.IN_PROGRESS);
        this.status = RideStatus.COMPLETED;
        touch();
    }

    public void cancel() {
        if (status == RideStatus.COMPLETED) {
            throw new IllegalStateException("Completed rides cannot be cancelled");
        }
        this.status = RideStatus.CANCELLED;
        touch();
    }

    private void ensureState(RideStatus required) {
        if (status != required) {
            throw new IllegalStateException("Expected status " + required + " but found " + status);
        }
    }
}
