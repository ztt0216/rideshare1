package com.swen90007.rideshare.domain.model;

import java.time.LocalDateTime;

public class Payment {
    private Integer id;
    private int rideId;
    private int payerUserId;   // rider
    private int payeeUserId;   // driver
    private double amount;
    private LocalDateTime createdAt;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public int getRideId() { return rideId; }
    public void setRideId(int rideId) { this.rideId = rideId; }

    public int getPayerUserId() { return payerUserId; }
    public void setPayerUserId(int payerUserId) { this.payerUserId = payerUserId; }

    public int getPayeeUserId() { return payeeUserId; }
    public void setPayeeUserId(int payeeUserId) { this.payeeUserId = payeeUserId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
