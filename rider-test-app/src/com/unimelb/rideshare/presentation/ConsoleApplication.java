package com.unimelb.rideshare.presentation;

import com.unimelb.rideshare.ApplicationContext;
import com.unimelb.rideshare.common.Result;
import com.unimelb.rideshare.domain.model.Driver;
import com.unimelb.rideshare.domain.model.Ride;
import com.unimelb.rideshare.domain.model.RideRequest;
import com.unimelb.rideshare.domain.model.Rider;
import com.unimelb.rideshare.domain.value.AvailabilityWindow;
import com.unimelb.rideshare.domain.value.Location;
import com.unimelb.rideshare.service.DriverService;
import com.unimelb.rideshare.service.RideService;
import com.unimelb.rideshare.service.RiderService;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Simple console facade for interacting with the reference implementation.
 */
public final class ConsoleApplication {
    private final ConsoleReader reader = new ConsoleReader();
    private final DriverService driverService;
    private final RiderService riderService;
    private final RideService rideService;

    public ConsoleApplication(ApplicationContext context) {
        this.driverService = context.getDriverService();
        this.riderService = context.getRiderService();
        this.rideService = context.getRideService();
    }

    public void run() {
        MenuOption option;
        do {
            printMenu();
            int choice = reader.readInt("Select option");
            try {
                option = MenuOption.fromCode(choice);
            } catch (IllegalArgumentException ex) {
                System.out.println(ex.getMessage());
                option = null;
                continue;
            }
            handleOption(option);
        } while (option != MenuOption.EXIT);
    }

    private void printMenu() {
        System.out.println();
        for (MenuOption option : MenuOption.values()) {
            System.out.printf("%2d - %s%n", option.getCode(), option.getLabel());
        }
    }

    private void handleOption(MenuOption option) {
        switch (option) {
            case REGISTER_DRIVER:
                registerDriver();
                break;
            case UPDATE_DRIVER_AVAILABILITY:
                updateDriverAvailability();
                break;
            case REGISTER_RIDER:
                registerRider();
                break;
            case REQUEST_RIDE:
                requestRide();
                break;
            case MATCH_RIDE:
                matchRide();
                break;
            case ACCEPT_RIDE:
                acceptRide();
                break;
            case START_RIDE:
                startRide();
                break;
            case COMPLETE_RIDE:
                completeRide();
                break;
            case CANCEL_REQUEST:
                cancelRideRequest();
                break;
            case LIST_DRIVERS:
                listDrivers();
                break;
            case LIST_OPEN_REQUESTS:
                listOpenRequests();
                break;
            case LIST_ACTIVE_RIDES:
                listActiveRides();
                break;
            case EXIT:
                System.out.println("Goodbye!");
                break;
            default:
                System.out.println("Unsupported option");
        }
    }

    private void registerDriver() {
        String name = reader.readLine("Driver name");
        String email = reader.readLine("Driver email");
        String make = reader.readLine("Vehicle make");
        String model = reader.readLine("Vehicle model");
        String colour = reader.readLine("Vehicle colour");
        String registration = reader.readLine("Vehicle registration");
        AvailabilityWindow window = new AvailabilityWindow(
                reader.readDay("Availability day"),
                reader.readTime("Availability start"),
                reader.readTime("Availability end")
        );
        Result<Driver> result = driverService.registerDriver(name, email,
                new com.unimelb.rideshare.domain.value.Vehicle(make, model, colour, registration));
        result.getValue().ifPresent(driver -> {
            driverService.updateAvailability(driver.getId(), List.of(window));
            System.out.println("Driver registered with id: " + driver.getId());
        });
        result.getMessage().ifPresent(System.out::println);
    }

    private void updateDriverAvailability() {
        UUID driverId = UUID.fromString(reader.readLine("Driver id"));
        int slotCount = reader.readInt("Number of availability slots");
        List<AvailabilityWindow> windows = new ArrayList<>();
        for (int i = 0; i < slotCount; i++) {
            DayOfWeek day = reader.readDay("Slot " + (i + 1) + " day");
            LocalTime start = reader.readTime("Slot " + (i + 1) + " start time");
            LocalTime end = reader.readTime("Slot " + (i + 1) + " end time");
            windows.add(new AvailabilityWindow(day, start, end));
        }
        Result<Driver> result = driverService.updateAvailability(driverId, windows);
        if (result.isSuccess()) {
            System.out.println("Availability updated");
        } else {
            result.getMessage().ifPresent(System.out::println);
        }
    }

    private void registerRider() {
        String name = reader.readLine("Rider name");
        String email = reader.readLine("Rider email");
        Result<Rider> result = riderService.registerRider(name, email);
        result.getValue().ifPresent(rider ->
                System.out.println("Rider registered with id: " + rider.getId()));
        result.getMessage().ifPresent(System.out::println);
    }

    private void requestRide() {
        UUID riderId = UUID.fromString(reader.readLine("Rider id"));
        String pickup = reader.readLine("Pickup location");
        String dropOff = reader.readLine("Drop off location");
        Location pickupLocation = Location.builder().description(pickup).build();
        Location dropOffLocation = Location.builder().description(dropOff).build();
        Result<RideRequest> result = rideService.requestRide(riderId, pickupLocation, dropOffLocation);
        if (result.isSuccess()) {
            System.out.println("Ride request id: " + result.getValue().get().getId());
        } else {
            result.getMessage().ifPresent(System.out::println);
        }
    }

    private void matchRide() {
        UUID requestId = UUID.fromString(reader.readLine("Ride request id"));
        Result<Ride> result = rideService.matchRide(requestId);
        if (result.isSuccess()) {
            Ride ride = result.getValue().get();
            System.out.printf("Ride %s matched to driver %s%n", ride.getId(), ride.getDriverId());
        } else {
            result.getMessage().ifPresent(System.out::println);
        }
    }

    private void acceptRide() {
        UUID rideId = UUID.fromString(reader.readLine("Ride id"));
        UUID driverId = UUID.fromString(reader.readLine("Driver id"));
        Result<Ride> result = rideService.acceptRide(rideId, driverId);
        if (result.isSuccess()) {
            System.out.println("Ride accepted");
        } else {
            result.getMessage().ifPresent(System.out::println);
        }
    }

    private void startRide() {
        UUID rideId = UUID.fromString(reader.readLine("Ride id"));
        Result<Ride> result = rideService.startRide(rideId);
        if (result.isSuccess()) {
            System.out.println("Ride started");
        } else {
            result.getMessage().ifPresent(System.out::println);
        }
    }

    private void completeRide() {
        UUID rideId = UUID.fromString(reader.readLine("Ride id"));
        Result<Ride> result = rideService.completeRide(rideId);
        if (result.isSuccess()) {
            System.out.println("Ride completed");
        } else {
            result.getMessage().ifPresent(System.out::println);
        }
    }

    private void cancelRideRequest() {
        UUID requestId = UUID.fromString(reader.readLine("Ride request id"));
        Result<RideRequest> result = rideService.cancelRequest(requestId);
        if (result.isSuccess()) {
            System.out.println("Ride request cancelled");
        } else {
            result.getMessage().ifPresent(System.out::println);
        }
    }

    private void listDrivers() {
        List<Driver> drivers = driverService.listDrivers();
        if (drivers.isEmpty()) {
            System.out.println("No drivers registered");
        } else {
            drivers.forEach(driver -> System.out.println(driver.getId() + " -> " + driver.getName()));
        }
    }

    private void listOpenRequests() {
        List<RideRequest> requests = rideService.listOpenRequests();
        if (requests.isEmpty()) {
            System.out.println("No open requests");
        } else {
            requests.forEach(req -> System.out.println(req.getId() + " -> " + req.getStatus()));
        }
    }

    private void listActiveRides() {
        List<Ride> rides = rideService.listActiveRides();
        if (rides.isEmpty()) {
            System.out.println("No active rides");
        } else {
            rides.forEach(ride -> System.out.println(ride.getId() + " -> " + ride.getStatus()));
        }
    }
}