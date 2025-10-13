package com.unimelb.rideshare.service.impl;

import com.unimelb.rideshare.service.NotificationService;

/**
 * Sends notifications by writing to stdout.
 */
public final class ConsoleNotificationService implements NotificationService {
    @Override
    public void notifyDriver(String email, String message) {
        System.out.printf("[Driver Notification] %s -> %s%n", email, message);
    }

    @Override
    public void notifyRider(String email, String message) {
        System.out.printf("[Rider Notification] %s -> %s%n", email, message);
    }
}
