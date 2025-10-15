package com.swen90007.rideshare.domain.model;

/** One wallet per user; use "amount" in Yuan only. */
public class Wallet {
    private int userId;
    private double balance; // 元

    public Wallet() {}

    public Wallet(int userId, double balance) {
        this.userId = userId;
        this.balance = balance;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    @Override
    public String toString() {
        return "Wallet{userId=" + userId + ", balance=" + balance + "}";
    }
}
