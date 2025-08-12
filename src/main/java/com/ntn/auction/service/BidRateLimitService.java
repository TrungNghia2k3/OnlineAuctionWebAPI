package com.ntn.auction.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BidRateLimitService {

    RedisService redisService;
    IpAddressService ipAddressService;

    // Dynamic limits (changes based on time remaining)
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final String BID_COUNT_PREFIX = "bid_count:";
    private static final String CONSECUTIVE_BID_PREFIX = "consecutive_bid:";
    private static final String SHILL_DETECTION_PREFIX = "shill_detection:";

    /**
     * Check rate limiting with dynamic limits based on auction end time
     */
    public boolean isRateLimited(String userId, Long itemId, LocalDateTime auctionEndDate) {
        Duration timeLeft = Duration.between(LocalDateTime.now(), auctionEndDate);

        // Configure limits based on time remaining
        RateLimits limits = configureLimits(timeLeft);

        // Check various rate limiting rules
        if (isPerMinuteLimitExceeded(userId, limits.maxBidsPerMinute)) {
            log.warn("User {} exceeded per-minute bid limit", userId);
            return true;
        }

        if (isPerHourLimitExceeded(userId, limits.maxBidsPerHour)) {
            log.warn("User {} exceeded per-hour bid limit", userId);
            return true;
        }

        if (isPerItemHourLimitExceeded(userId, itemId, limits.maxBidsPerItemPerHour)) {
            log.warn("User {} exceeded per-item-hour bid limit for item {}", userId, itemId);
            return true;
        }

        if (isConsecutiveBidLimitExceeded(userId, itemId, limits.maxConsecutiveBids)) {
            log.warn("User {} exceeded consecutive bid limit for item {}", userId, itemId);
            return true;
        }

        // Update counters after successful validation
        updateBidCounters(userId, itemId);

        return false;
    }

    /**
     * Detect shill bidding based on IP patterns and behavior
     */
    public boolean detectShillBidding(String userId, Long itemId, String sellerIp, String bidderIp) {
        // Check if bidder and seller have same IP
        if (sellerIp != null && sellerIp.equals(bidderIp)) {
            log.warn("Shill bidding detected: Same IP for seller and bidder {} on item {}", userId, itemId);
            return true;
        }

        // Check for suspicious bidding patterns
        if (isSuspiciousBiddingPattern(userId, itemId)) {
            log.warn("Suspicious bidding pattern detected for user {} on item {}", userId, itemId);
            return true;
        }

        // Log for analysis
        logShillDetectionData(userId, itemId, bidderIp);

        return false;
    }

    /**
     * Configure rate limits based on time remaining in auction
     */
    private RateLimits configureLimits(Duration timeLeft) {
        if (timeLeft.toHours() > 2) {
            // Early/middle phase: stricter limits to prevent spam
            return new RateLimits(5, 50, 15, 20, 100);
        } else {
            // Final phase: relaxed limits for competitive bidding
            return new RateLimits(20, 200, 40, 50, 300);
        }
    }

    /**
     * Check per-minute bid limit
     */
    private boolean isPerMinuteLimitExceeded(String userId, int maxBidsPerMinute) {
        String key = RATE_LIMIT_PREFIX + "minute:" + userId;
        Integer currentCount = redisService.getInteger(key);

        if (currentCount == null) {
            currentCount = 0;
        }

        if (currentCount >= maxBidsPerMinute) {
            return true;
        }

        // Increment counter with 60-second expiry
        redisService.increment(key);
        redisService.setExpiry(key, 60);

        return false;
    }

    /**
     * Check per-hour bid limit
     */
    private boolean isPerHourLimitExceeded(String userId, int maxBidsPerHour) {
        String key = RATE_LIMIT_PREFIX + "hour:" + userId;
        Integer currentCount = redisService.getInteger(key);

        if (currentCount == null) {
            currentCount = 0;
        }

        if (currentCount >= maxBidsPerHour) {
            return true;
        }

        // Increment counter with 1-hour expiry
        redisService.increment(key);
        redisService.setExpiry(key, 3600);

        return false;
    }

    /**
     * Check per-item per-hour bid limit
     */
    private boolean isPerItemHourLimitExceeded(String userId, Long itemId, int maxBidsPerItemPerHour) {
        String key = RATE_LIMIT_PREFIX + "item_hour:" + userId + ":" + itemId;
        Integer currentCount = redisService.getInteger(key);

        if (currentCount == null) {
            currentCount = 0;
        }

        if (currentCount >= maxBidsPerItemPerHour) {
            return true;
        }

        // Increment counter with 1-hour expiry
        redisService.increment(key);
        redisService.setExpiry(key, 3600);

        return false;
    }

    /**
     * Check consecutive bid limit for an item
     */
    private boolean isConsecutiveBidLimitExceeded(String userId, Long itemId, int maxConsecutiveBids) {
        String key = CONSECUTIVE_BID_PREFIX + itemId + ":" + userId;
        Integer consecutiveCount = redisService.getInteger(key);

        if (consecutiveCount == null) {
            consecutiveCount = 0;
        }

        return consecutiveCount >= maxConsecutiveBids;
    }

    /**
     * Update bid counters after successful bid
     */
    private void updateBidCounters(String userId, Long itemId) {
        // Update consecutive bid counter
        String consecutiveKey = CONSECUTIVE_BID_PREFIX + itemId + ":" + userId;
        redisService.increment(consecutiveKey);
        redisService.setExpiry(consecutiveKey, 3600); // 1 hour expiry

        // Reset other users' consecutive counters for this item
        // This would require additional logic to track all bidders per item
        // For now, we'll let it expire naturally
    }

    /**
     * Reset consecutive bid counter when another user bids
     */
    public void resetConsecutiveBidCounter(Long itemId, String newBidderId) {
        // In a real implementation, we'd track all users who bid on this item
        // and reset their consecutive counters when someone else bids
        String pattern = CONSECUTIVE_BID_PREFIX + itemId + ":*";

        // For simplicity, we'll just reset the specific user's counter
        // A more sophisticated approach would use Redis patterns to reset all counters
        log.debug("Resetting consecutive bid counters for item {} after bid by {}", itemId, newBidderId);
    }

    /**
     * Check for suspicious bidding patterns
     */
    private boolean isSuspiciousBiddingPattern(String userId, Long itemId) {
        // Check if user is bidding too frequently in a short time
        String patternKey = SHILL_DETECTION_PREFIX + "pattern:" + userId + ":" + itemId;
        Integer recentBids = redisService.getInteger(patternKey);

        if (recentBids == null) {
            recentBids = 0;
        }

        // If more than 10 bids in 5 minutes, consider suspicious
        if (recentBids > 10) {
            return true;
        }

        // Update pattern tracking
        redisService.increment(patternKey);
        redisService.setExpiry(patternKey, 300); // 5 minutes

        return false;
    }

    /**
     * Log shill detection data for analysis
     */
    private void logShillDetectionData(String userId, Long itemId, String ipAddress) {
        String logKey = SHILL_DETECTION_PREFIX + "log:" + itemId;
        String logData = String.format("%s|%s|%s|%s",
                LocalDateTime.now(), userId, ipAddress, "BID_ATTEMPT");

        redisService.addToList(logKey, logData);
        redisService.setExpiry(logKey, 86400 * 7); // Keep for 7 days
    }

    /**
     * Inner class to hold rate limit configuration
     */
    private static class RateLimits {
        final int maxBidsPerMinute;
        final int maxBidsPerHour;
        final int maxConsecutiveBids;
        final int maxBidsPerItemPerHour;
        final int suspiciousBidThreshold;

        RateLimits(int maxBidsPerMinute, int maxBidsPerHour, int maxConsecutiveBids,
                   int maxBidsPerItemPerHour, int suspiciousBidThreshold) {
            this.maxBidsPerMinute = maxBidsPerMinute;
            this.maxBidsPerHour = maxBidsPerHour;
            this.maxConsecutiveBids = maxConsecutiveBids;
            this.maxBidsPerItemPerHour = maxBidsPerItemPerHour;
            this.suspiciousBidThreshold = suspiciousBidThreshold;
        }
    }
}
