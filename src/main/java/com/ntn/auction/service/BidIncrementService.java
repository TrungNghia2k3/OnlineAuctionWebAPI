package com.ntn.auction.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class BidIncrementService {

    /**
     * Calculate minimum bid increment based on current bid price
     * Implements dynamic pricing tiers according to business rules:
     * - $1–$49.99 → $1 increment
     * - $50–$199.99 → $5 increment
     * - $200–$999.99 → $10 increment
     * - $1,000-$4999.99 → $50 increment
     * - >$5,000 → $100 increment
     */
    public BigDecimal calculateMinIncrement(BigDecimal currentPrice) {
        if (currentPrice == null) {
            return BigDecimal.ONE; // Default $1 increment
        }

        // Define pricing tiers
        if (currentPrice.compareTo(new BigDecimal("49.99")) <= 0) {
            return BigDecimal.ONE; // $1
        } else if (currentPrice.compareTo(new BigDecimal("199.99")) <= 0) {
            return new BigDecimal("5.00"); // $5
        } else if (currentPrice.compareTo(new BigDecimal("999.99")) <= 0) {
            return new BigDecimal("10.00"); // $10
        } else if (currentPrice.compareTo(new BigDecimal("4999.99")) <= 0) {
            return new BigDecimal("50.00"); // $50
        } else {
            return new BigDecimal("100.00"); // $100
        }
    }

    /**
     * Calculate the minimum valid bid amount for an item
     * @param currentPrice Current highest bid or starting price
     * @return Minimum valid bid amount
     */
    public BigDecimal calculateMinimumBid(BigDecimal currentPrice) {
        BigDecimal increment = calculateMinIncrement(currentPrice);
        return currentPrice.add(increment);
    }

    /**
     * Validate if a bid amount meets the minimum increment requirement
     */
    public boolean isValidBidAmount(BigDecimal currentPrice, BigDecimal bidAmount) {
        BigDecimal minimumBid = calculateMinimumBid(currentPrice);
        return bidAmount.compareTo(minimumBid) >= 0;
    }

    /**
     * Get the increment tier description for display purposes
     */
    public String getIncrementTierDescription(BigDecimal currentPrice) {
        BigDecimal increment = calculateMinIncrement(currentPrice);

        if (increment.equals(BigDecimal.ONE)) {
            return "Tier 1: $1-$49.99 (Increment: $1)";
        } else if (increment.equals(new BigDecimal("5.00"))) {
            return "Tier 2: $50-$199.99 (Increment: $5)";
        } else if (increment.equals(new BigDecimal("10.00"))) {
            return "Tier 3: $200-$999.99 (Increment: $10)";
        } else if (increment.equals(new BigDecimal("50.00"))) {
            return "Tier 4: $1,000-$4,999.99 (Increment: $50)";
        } else {
            return "Tier 5: $5,000+ (Increment: $100)";
        }
    }
}
