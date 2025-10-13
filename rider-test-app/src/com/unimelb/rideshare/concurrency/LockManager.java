package com.unimelb.rideshare.concurrency;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provides fine-grained locking around ride related aggregates so concurrent operations are safe.
 */
public final class LockManager {
    private final ConcurrentHashMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    public Lock acquire(UUID id) {
        ReentrantLock lock = locks.computeIfAbsent(id, unused -> new ReentrantLock());
        lock.lock();
        return lock;
    }

    public boolean tryAcquire(UUID id, long timeout, TimeUnit unit) throws InterruptedException {
        ReentrantLock lock = locks.computeIfAbsent(id, unused -> new ReentrantLock());
        return lock.tryLock(timeout, unit);
    }

    public void release(UUID id) {
        ReentrantLock lock = locks.get(id);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
