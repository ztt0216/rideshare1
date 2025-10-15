package com.swen90007.rideshare.domain.model;

/**
 * Optional enum to improve readability.
 * We still persist String statuses in DB and compare them in controllers/services.
 */
public enum RideStatus {
    REQUESTED,
    ACCEPTED,
    ENROUTE,
    COMPLETED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
