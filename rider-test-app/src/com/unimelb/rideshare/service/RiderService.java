package com.unimelb.rideshare.service;

import com.unimelb.rideshare.common.Result;
import com.unimelb.rideshare.domain.model.Rider;

import java.util.Optional;
import java.util.UUID;

/**
 * Rider onboarding and lookup operations.
 */
public interface RiderService {
    Result<Rider> registerRider(String name, String email);

    Optional<Rider> findById(UUID riderId);
}
