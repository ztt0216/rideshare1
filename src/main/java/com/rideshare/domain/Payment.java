package com.rideshare.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class Payment {
    private Long id;
    private Long rideId;
    private BigDecimal amount;
    private Instant paymentTime;

    public Payment(Long rideId, BigDecimal amount) {
        this.rideId = rideId;
        this.amount = amount;
        this.paymentTime = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRideId() {
        return rideId;
    }

    public void setRideId(Long rideId) {
        this.rideId = rideId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Instant getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(Instant paymentTime) {
        this.paymentTime = paymentTime;
    }
}
