package com.rideshare.service;

import com.rideshare.util.RideShareException;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FareCalculationServiceImpl implements FareCalculationService {
    
    private static final BigDecimal AIRPORT_FARE = new BigDecimal("60.00");
    private static final BigDecimal INTERSTATE_FARE = new BigDecimal("500.00");
    private static final BigDecimal REGIONAL_FARE = new BigDecimal("220.00");
    private static final BigDecimal METRO_FARE = new BigDecimal("40.00");
    
    private static final Pattern POSTCODE_PATTERN = Pattern.compile("\\b(\\d{4})\\b");

    @Override
    public BigDecimal calculateFare(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            throw new RideShareException("Destination cannot be empty");
        }
        
        // Extract postcode from destination string
        Matcher matcher = POSTCODE_PATTERN.matcher(destination);
        if (!matcher.find()) {
            throw new RideShareException("No valid postcode found in destination: " + destination);
        }
        
        String postcodeStr = matcher.group(1);
        int postcode = Integer.parseInt(postcodeStr);
        
        // Apply fare rules
        if (postcode == 3045) {
            // Airport
            return AIRPORT_FARE;
        } else if (postcode < 3000 || postcode > 3999) {
            // Interstate (non-3xxx)
            return INTERSTATE_FARE;
        } else if (postcode >= 3300 && postcode <= 3999) {
            // Regional (3300-3999)
            return REGIONAL_FARE;
        } else if (postcode >= 3000 && postcode <= 3299) {
            // Metro (3000-3299)
            return METRO_FARE;
        }
        
        throw new RideShareException("Unable to determine fare for postcode: " + postcode);
    }
}
