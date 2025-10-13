package com.unimelb.rideshare.domain.value;

import java.util.Objects;

/**
 * Simple representation of a geo location with optional latitude/longitude metadata.
 */
public final class Location {
    private final String description;
    private final Double latitude;
    private final Double longitude;

    private Location(Builder builder) {
        this.description = builder.description;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
    }

    public String getDescription() {
        return description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    @Override
    public String toString() {
        return description +
                (latitude != null && longitude != null ?
                        String.format(" (%.5f, %.5f)", latitude, longitude) : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Location)) {
            return false;
        }
        Location location = (Location) o;
        return Objects.equals(description, location.description)
                && Objects.equals(latitude, location.latitude)
                && Objects.equals(longitude, location.longitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, latitude, longitude);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String description;
        private Double latitude;
        private Double longitude;

        private Builder() {
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public Builder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public Location build() {
            if (description == null || description.isBlank()) {
                throw new IllegalStateException("Location description must be provided");
            }
            return new Location(this);
        }
    }
}
