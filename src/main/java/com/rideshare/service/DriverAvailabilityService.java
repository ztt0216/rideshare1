package com.rideshare.service;

import com.rideshare.domain.DriverAvailability;
import java.time.LocalTime;
import java.util.List;

public interface DriverAvailabilityService {
    void setAvailability(Long driverId, List<AvailabilitySchedule> schedules);
    List<DriverAvailability> getAvailability(Long driverId);
    boolean isDriverAvailable(Long driverId);
    void clearAvailability(Long driverId);
    
    // Helper class for schedule input
    class AvailabilitySchedule {
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        
        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
    }
}
