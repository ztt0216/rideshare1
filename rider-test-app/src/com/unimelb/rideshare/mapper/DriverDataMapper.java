package com.unimelb.rideshare.mapper;

import com.unimelb.rideshare.datasource.DataStore;
import com.unimelb.rideshare.domain.model.Driver;
import com.unimelb.rideshare.domain.value.AvailabilitySchedule;
import com.unimelb.rideshare.domain.value.AvailabilityWindow;
import com.unimelb.rideshare.domain.value.Vehicle;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Maps driver domain objects to the data store.
 */
public final class DriverDataMapper extends AbstractDataMapper<Driver> {
    public static final String COLLECTION = "drivers";

    public DriverDataMapper(DataStore dataStore) {
        super(dataStore, COLLECTION);
    }

    @Override
    protected Map<String, Object> toDocument(Driver entity) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", entity.getId());
        doc.put("name", entity.getName());
        doc.put("email", entity.getEmail());
        if (entity.getVehicle() != null) {
            Map<String, Object> vehicle = new HashMap<>();
            vehicle.put("make", entity.getVehicle().getMake());
            vehicle.put("model", entity.getVehicle().getModel());
            vehicle.put("colour", entity.getVehicle().getColour());
            vehicle.put("registration", entity.getVehicle().getRegistrationNumber());
            doc.put("vehicle", vehicle);
        }
        if (entity.getAvailabilitySchedule() != null) {
            List<Map<String, Object>> schedule = entity.getAvailabilitySchedule().getWindows().stream()
                    .map(window -> {
                        Map<String, Object> entry = new HashMap<>();
                        entry.put("day", window.getDayOfWeek().name());
                        entry.put("start", window.getStart().toString());
                        entry.put("end", window.getEnd().toString());
                        return entry;
                    })
                    .collect(Collectors.toList());
            doc.put("schedule", schedule);
        }
        return doc;
    }

    @Override
    protected Driver fromDocument(Map<String, Object> document) {
        UUID id = (UUID) document.get("id");
        String name = (String) document.get("name");
        String email = (String) document.get("email");
        Vehicle vehicle = null;
        if (document.containsKey("vehicle")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> vehicleDoc = (Map<String, Object>) document.get("vehicle");
            vehicle = new Vehicle(
                    (String) vehicleDoc.get("make"),
                    (String) vehicleDoc.get("model"),
                    (String) vehicleDoc.get("colour"),
                    (String) vehicleDoc.get("registration")
            );
        }
        AvailabilitySchedule schedule = new AvailabilitySchedule();
        if (document.containsKey("schedule")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) document.get("schedule");
            for (Map<String, Object> entry : entries) {
                DayOfWeek day = DayOfWeek.valueOf((String) entry.get("day"));
                LocalTime start = LocalTime.parse((String) entry.get("start"));
                LocalTime end = LocalTime.parse((String) entry.get("end"));
                schedule.addWindow(new AvailabilityWindow(day, start, end));
            }
        }
        return new Driver(id, name, email, vehicle, schedule);
    }
}
