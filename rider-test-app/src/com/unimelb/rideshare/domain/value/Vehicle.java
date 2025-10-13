package com.unimelb.rideshare.domain.value;

import java.util.Objects;

/**
 * Vehicle metadata attached to a driver.
 */
public final class Vehicle {
    private final String make;
    private final String model;
    private final String colour;
    private final String registrationNumber;

    public Vehicle(String make, String model, String colour, String registrationNumber) {
        this.make = requireNonBlank(make, "Vehicle make");
        this.model = requireNonBlank(model, "Vehicle model");
        this.colour = requireNonBlank(colour, "Vehicle colour");
        this.registrationNumber = requireNonBlank(registrationNumber, "Vehicle registration number");
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getColour() {
        return colour;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    @Override
    public String toString() {
        return colour + " " + make + " " + model + " (" + registrationNumber + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vehicle)) {
            return false;
        }
        Vehicle vehicle = (Vehicle) o;
        return Objects.equals(make, vehicle.make)
                && Objects.equals(model, vehicle.model)
                && Objects.equals(colour, vehicle.colour)
                && Objects.equals(registrationNumber, vehicle.registrationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(make, model, colour, registrationNumber);
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }
}
