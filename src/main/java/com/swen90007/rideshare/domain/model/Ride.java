package com.swen90007.rideshare.domain.model;

/**
 * Ride aggregate.
 * status uses String for simplicity and DB alignment (see RideStatus for enum).
 */
public class Ride {
    private int id;
    private int riderId;
    private Integer driverId;     // nullable until accepted
    private String pickup;        // postcode/address as stored in DB
    private String destination;   // postcode/address
    private String status;        // "REQUESTED","ACCEPTED","ENROUTE","COMPLETED","CANCELLED"
    private double fare;          // fare decided at request time
    private long requestedAt;     // epoch millis

    public Ride() { }

    public Ride(int id, int riderId, Integer driverId,
                String pickup, String destination, String status,
                double fare, long requestedAt) {
        this.id = id;
        this.riderId = riderId;
        this.driverId = driverId;
        this.pickup = pickup;
        this.destination = destination;
        this.status = status;
        this.fare = fare;
        this.requestedAt = requestedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getRiderId() { return riderId; }
    public void setRiderId(int riderId) { this.riderId = riderId; }

    public Integer getDriverId() { return driverId; }
    public void setDriverId(Integer driverId) { this.driverId = driverId; }

    public String getPickup() { return pickup; }
    public void setPickup(String pickup) { this.pickup = pickup; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getFare() { return fare; }
    public void setFare(double fare) { this.fare = fare; }

    public long getRequestedAt() { return requestedAt; }
    public void setRequestedAt(long requestedAt) { this.requestedAt = requestedAt; }




    @Override
    public String toString() {
        return "Ride{id=" + id +
                ", riderId=" + riderId +
                ", driverId=" + driverId +
                ", pickup='" + pickup + '\'' +
                ", destination='" + destination + '\'' +
                ", status='" + status + '\'' +
                ", fare=" + fare +
                ", requestedAt=" + requestedAt + "}";
    }
}
