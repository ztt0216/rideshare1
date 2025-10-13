package com.unimelb.rideshare.domain.value;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregates weekly availability windows for a driver.
 */
public final class AvailabilitySchedule {
    private final List<AvailabilityWindow> windows = new ArrayList<>();

    public AvailabilitySchedule() {
    }

    public AvailabilitySchedule(List<AvailabilityWindow> windows) {
        if (windows != null) {
            for (AvailabilityWindow window : windows) {
                addWindow(window);
            }
        }
    }

    public void addWindow(AvailabilityWindow window) {
        Objects.requireNonNull(window, "Availability window cannot be null");
        windows.add(window);
    }

    public void clear() {
        windows.clear();
    }

    public List<AvailabilityWindow> getWindows() {
        return Collections.unmodifiableList(windows);
    }

    public boolean isAvailable(DayOfWeek dayOfWeek, LocalTime time) {
        return windows.stream().anyMatch(w -> w.getDayOfWeek() == dayOfWeek && w.covers(time));
    }

    @Override
    public String toString() {
        return windows.toString();
    }
}
