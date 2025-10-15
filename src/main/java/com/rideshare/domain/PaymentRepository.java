package com.rideshare.domain;

public interface PaymentRepository {
    Long insert(Payment payment);
    Payment findById(Long id);
    Payment findByRideId(Long rideId);
}
