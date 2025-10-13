package com.unimelb.rideshare.service;

/**
 * Outbound notification abstraction.
 */
public interface NotificationService {
    void notifyDriver(String email, String message);

    void notifyRider(String email, String message);
}
