package com.unimelb.rideshare.domain.model;

import com.unimelb.rideshare.domain.value.AvailabilitySchedule;
import com.unimelb.rideshare.domain.value.Vehicle;

import java.util.UUID;

/**
 * Driver participant capable of fulfilling ride requests.
 */
public final class Driver extends User {
    private Vehicle vehicle;
    private AvailabilitySchedule availabilitySchedule;

    public Driver(UUID id, String name, String email, Vehicle vehicle, AvailabilitySchedule availabilitySchedule) {
        super(id, name, email);
        this.vehicle = vehicle;
        this.availabilitySchedule = availabilitySchedule == null ? new AvailabilitySchedule() : availabilitySchedule;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
        touch();
    }

    public AvailabilitySchedule getAvailabilitySchedule() {
        return availabilitySchedule;
    }

    public void setAvailabilitySchedule(AvailabilitySchedule availabilitySchedule) {
        this.availabilitySchedule = availabilitySchedule;
        touch();
    }
}
