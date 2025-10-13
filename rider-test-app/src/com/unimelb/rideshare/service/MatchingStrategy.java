package com.unimelb.rideshare.service;

import com.unimelb.rideshare.domain.model.Driver;
import com.unimelb.rideshare.domain.model.RideRequest;

import java.util.Optional;

/**
 * Strategy interface for assigning drivers to ride requests.
 */
public interface MatchingStrategy {
    Optional<Driver> selectDriver(RideRequest request);
}
