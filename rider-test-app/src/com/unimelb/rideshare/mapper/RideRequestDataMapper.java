package com.unimelb.rideshare.mapper;

import com.unimelb.rideshare.datasource.DataStore;
import com.unimelb.rideshare.domain.model.RideRequest;
import com.unimelb.rideshare.domain.model.RideStatus;
import com.unimelb.rideshare.domain.value.Location;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper for ride request entities.
 */
public final class RideRequestDataMapper extends AbstractDataMapper<RideRequest> {
    public static final String COLLECTION = "ride_requests";

    public RideRequestDataMapper(DataStore dataStore) {
        super(dataStore, COLLECTION);
    }

    @Override
    protected Map<String, Object> toDocument(RideRequest entity) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", entity.getId());
        doc.put("riderId", entity.getRiderId());
        doc.put("pickup", entity.getPickup().getDescription());
        doc.put("dropOff", entity.getDropOff().getDescription());
        doc.put("requestedAt", entity.getRequestedAt().toString());
        doc.put("status", entity.getStatus().name());
        doc.put("driverId", entity.getAllocatedDriverId());
        return doc;
    }

    @Override
    protected RideRequest fromDocument(Map<String, Object> document) {
        UUID id = (UUID) document.get("id");
        UUID riderId = (UUID) document.get("riderId");
        Location pickup = Location.builder().description((String) document.get("pickup")).build();
        Location dropOff = Location.builder().description((String) document.get("dropOff")).build();
        LocalDateTime requestedAt = LocalDateTime.parse((String) document.get("requestedAt"));
        RideRequest rideRequest = new RideRequest(id, riderId, pickup, dropOff, requestedAt);
        UUID driverId = (UUID) document.get("driverId");
        if (driverId != null) {
            rideRequest.assignDriver(driverId);
        }
        RideStatus storedStatus = RideStatus.valueOf((String) document.get("status"));
        switch (storedStatus) {
            case ACCEPTED:
                rideRequest.markAccepted();
                break;
            case IN_PROGRESS:
                rideRequest.markAccepted();
                rideRequest.markInProgress();
                break;
            case COMPLETED:
                rideRequest.markAccepted();
                rideRequest.markInProgress();
                rideRequest.markCompleted();
                break;
            case CANCELLED:
                rideRequest.cancel();
                break;
            default:
                break;
        }
        return rideRequest;
    }
}
