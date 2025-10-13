package com.unimelb.rideshare.domain.value;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Represents a recurring weekly availability slot for a driver.
 */
public final class AvailabilityWindow {
    private final DayOfWeek dayOfWeek;
    private final LocalTime start;
    private final LocalTime end;

    public AvailabilityWindow(DayOfWeek dayOfWeek, LocalTime start, LocalTime end) {
        if (dayOfWeek == null) {
            throw new IllegalArgumentException("Day of week is required");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end times are required");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        this.dayOfWeek = dayOfWeek;
        this.start = start;
        this.end = end;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStart() {
        return start;
    }

    public LocalTime getEnd() {
        return end;
    }

    public boolean covers(LocalTime time) {
        return !time.isBefore(start) && !time.isAfter(end);
    }

    @Override
    public String toString() {
        return dayOfWeek + " " + start + "-" + end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AvailabilityWindow)) {
            return false;
        }
        AvailabilityWindow that = (AvailabilityWindow) o;
        return dayOfWeek == that.dayOfWeek && Objects.equals(start, that.start) && Objects.equals(end, that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, start, end);
    }
}
