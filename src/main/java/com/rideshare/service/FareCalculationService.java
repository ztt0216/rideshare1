package com.rideshare.service;

import java.math.BigDecimal;

public interface FareCalculationService {
    /**
     * Calculate fare based on destination postcode
     * Airport: $60 (postcode 3045)
     * Interstate: $500 (non-3xxx postcodes)
     * Regional: $220 (postcodes 3300-3999)
     * Metro: $40 (postcodes 3000-3299)
     */
    BigDecimal calculateFare(String destination);
}
