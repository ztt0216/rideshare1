package com.unimelb.rideshare;

import com.unimelb.rideshare.concurrency.DriverAvailabilityGuard;
import com.unimelb.rideshare.concurrency.LockManager;
import com.unimelb.rideshare.datasource.DataStore;
import com.unimelb.rideshare.datasource.InMemoryDataStore;
import com.unimelb.rideshare.domain.model.Driver;
import com.unimelb.rideshare.domain.model.Ride;
import com.unimelb.rideshare.domain.model.RideRequest;
import com.unimelb.rideshare.domain.model.Rider;
import com.unimelb.rideshare.domain.value.AvailabilityWindow;
import com.unimelb.rideshare.domain.value.Vehicle;
import com.unimelb.rideshare.mapper.DriverDataMapper;
import com.unimelb.rideshare.mapper.MapperRegistry;
import com.unimelb.rideshare.mapper.RideDataMapper;
import com.unimelb.rideshare.mapper.RideRequestDataMapper;
import com.unimelb.rideshare.mapper.RiderDataMapper;
import com.unimelb.rideshare.service.DriverService;
import com.unimelb.rideshare.service.MatchingStrategy;
import com.unimelb.rideshare.service.NotificationService;
import com.unimelb.rideshare.service.RideService;
import com.unimelb.rideshare.service.RiderService;
import com.unimelb.rideshare.service.impl.AvailabilityMatchingStrategy;
import com.unimelb.rideshare.service.impl.ConsoleNotificationService;
import com.unimelb.rideshare.service.impl.DriverServiceImpl;
import com.unimelb.rideshare.service.impl.RideServiceImpl;
import com.unimelb.rideshare.service.impl.RiderServiceImpl;
import com.unimelb.rideshare.unitofwork.UnitOfWorkFactory;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Shared bootstrap class that wires together the layered components so both the console
 * application and the HTTP server reuse the same service instances.
 */
public final class ApplicationContext {
    private final DriverService driverService;
    private final RiderService riderService;
    private final RideService rideService;

    private final DriverDataMapper driverMapper;
    private final RiderDataMapper riderMapper;
    private final RideRequestDataMapper rideRequestMapper;
    private final RideDataMapper rideMapper;

    private final DriverAvailabilityGuard availabilityGuard;

    private ApplicationContext() {
        DataStore dataStore = new InMemoryDataStore();
        driverMapper = new DriverDataMapper(dataStore);
        riderMapper = new RiderDataMapper(dataStore);
        rideRequestMapper = new RideRequestDataMapper(dataStore);
        rideMapper = new RideDataMapper(dataStore);

        MapperRegistry registry = new MapperRegistry();
        registry.register(Driver.class, driverMapper);
        registry.register(Rider.class, riderMapper);
        registry.register(RideRequest.class, rideRequestMapper);
        registry.register(Ride.class, rideMapper);

        availabilityGuard = new DriverAvailabilityGuard();
        UnitOfWorkFactory unitOfWorkFactory = new UnitOfWorkFactory(registry);
        MatchingStrategy matchingStrategy = new AvailabilityMatchingStrategy(driverMapper, availabilityGuard);
        NotificationService notificationService = new ConsoleNotificationService();
        LockManager lockManager = new LockManager();

        driverService = new DriverServiceImpl(driverMapper, unitOfWorkFactory, availabilityGuard);
        riderService = new RiderServiceImpl(riderMapper, unitOfWorkFactory);
        rideService = new RideServiceImpl(
                riderMapper,
                driverMapper,
                rideRequestMapper,
                rideMapper,
                matchingStrategy,
                notificationService,
                unitOfWorkFactory,
                lockManager
        );

        seedSampleData();
    }

    public static ApplicationContext bootstrap() {
        return new ApplicationContext();
    }

    public DriverService getDriverService() {
        return driverService;
    }

    public RiderService getRiderService() {
        return riderService;
    }

    public RideService getRideService() {
        return rideService;
    }

    public DriverDataMapper getDriverMapper() {
        return driverMapper;
    }

    public RiderDataMapper getRiderMapper() {
        return riderMapper;
    }

    public RideRequestDataMapper getRideRequestMapper() {
        return rideRequestMapper;
    }

    public RideDataMapper getRideMapper() {
        return rideMapper;
    }

    private void seedSampleData() {
        driverService.registerDriver(
                "Sample Driver",
                "driver@example.com",
                new Vehicle("Toyota", "Camry", "Blue", "ABC123")
        ).getValue().ifPresent(driver -> driverService.updateAvailability(
                driver.getId(),
                List.of(
                        new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                        new AvailabilityWindow(DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(18, 0))
                )
        ));

        riderService.registerRider("Sample Rider", "rider@example.com");
    }
}