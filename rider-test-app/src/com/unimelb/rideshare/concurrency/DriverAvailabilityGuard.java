package com.unimelb.rideshare.concurrency;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Coordinates access to driver availability information so that updates and matches do not step on each other.
 */
public final class DriverAvailabilityGuard {
    private final ConcurrentHashMap<UUID, ReadWriteLock> locks = new ConcurrentHashMap<>();

    public void registerDriver(UUID driverId) {
        locks.computeIfAbsent(driverId, ignored -> new ReentrantReadWriteLock());
    }

    public <T> T withReadLock(UUID driverId, Supplier<T> action) {
        ReadWriteLock lock = locks.computeIfAbsent(driverId, ignored -> new ReentrantReadWriteLock());
        lock.readLock().lock();
        try {
            return action.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T> T withWriteLock(UUID driverId, Supplier<T> action) {
        ReadWriteLock lock = locks.computeIfAbsent(driverId, ignored -> new ReentrantReadWriteLock());
        lock.writeLock().lock();
        try {
            return action.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void withWriteLock(UUID driverId, Runnable action) {
        withWriteLock(driverId, () -> {
            action.run();
            return null;
        });
    }
}