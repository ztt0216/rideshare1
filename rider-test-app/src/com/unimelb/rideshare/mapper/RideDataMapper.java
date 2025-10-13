package com.unimelb.rideshare.mapper;

import com.unimelb.rideshare.datasource.DataStore;
import com.unimelb.rideshare.domain.model.Ride;
import com.unimelb.rideshare.domain.model.RideStatus;
import com.unimelb.rideshare.domain.value.Location;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper for persisted rides.
 */
public final class RideDataMapper extends AbstractDataMapper<Ride> {
    public static final String COLLECTION = "rides";

    public RideDataMapper(DataStore dataStore) {
        super(dataStore, COLLECTION);
    }

    @Override
    protected Map<String, Object> toDocument(Ride entity) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", entity.getId());
        doc.put("rideRequestId", entity.getRideRequestId());
        doc.put("driverId", entity.getDriverId());
        doc.put("riderId", entity.getRiderId());
        doc.put("pickup", entity.getPickup().getDescription());
        doc.put("dropOff", entity.getDropOff().getDescription());
        doc.put("scheduledStart", entity.getScheduledStart().toString());
        if (entity.getActualStart() != null) {
            doc.put("actualStart", entity.getActualStart().toString());
        }
        if (entity.getCompletedAt() != null) {
            doc.put("completedAt", entity.getCompletedAt().toString());
        }
        doc.put("status", entity.getStatus().name());
        return doc;
    }

    @Override
    protected Ride fromDocument(Map<String, Object> document) {
        UUID id = (UUID) document.get("id");
        UUID rideRequestId = (UUID) document.get("rideRequestId");
        UUID driverId = (UUID) document.get("driverId");
        UUID riderId = (UUID) document.get("riderId");
        Location pickup = Location.builder().description((String) document.get("pickup")).build();
        Location dropOff = Location.builder().description((String) document.get("dropOff")).build();
        LocalDateTime scheduledStart = LocalDateTime.parse((String) document.get("scheduledStart"));
        Ride ride = new Ride(id, rideRequestId, driverId, riderId, pickup, dropOff, scheduledStart);
        LocalDateTime actualStart = document.containsKey("actualStart")
                ? LocalDateTime.parse((String) document.get("actualStart"))
                : null;
        LocalDateTime completedAt = document.containsKey("completedAt")
                ? LocalDateTime.parse((String) document.get("completedAt"))
                : null;
        RideStatus status = RideStatus.valueOf((String) document.get("status"));
        ride.restoreState(status, actualStart, completedAt);
        return ride;
    }
}
