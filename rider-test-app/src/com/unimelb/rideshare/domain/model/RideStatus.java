package com.unimelb.rideshare.domain.model;

/**
 * Lifecycle states that a ride can progress through.
 */
public enum RideStatus {
    REQUESTED,
    MATCHED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
