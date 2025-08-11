package com.ntn.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidRateLimitService {

    private final RedisService redisService;
    private final IpAddressService ipAddressService;

    // Configuration constants
    private static final int MAX_BIDS_PER_MINUTE = 5;
    private static final int MAX_BIDS_PER_HOUR = 30;
    private static final int SUSPICIOUS_BID_THRESHOLD = 50;
    private static final int MAX_CONSECUTIVE_BIDS = 10;

    public boolean isRateLimited(String userId, Long itemId) {
        String minuteKey = "bid_rate:minute:" + userId;
        String hourKey = "bid_rate:hour:" + userId;
        String itemKey = "bid_rate:item:" + userId + ":" + itemId;

        // Check minute limit
        Long minuteBids = redisService.incrementValue(minuteKey, Duration.ofMinutes(1));

        // Check hour limit
        Long hourBids = redisService.incrementValue(hourKey, Duration.ofHours(1));

        // Check item-specific limit (prevent focus attacks)
        Long itemBids = redisService.incrementValue(itemKey, Duration.ofHours(1));

        // Flag suspicious activity
        if (hourBids > SUSPICIOUS_BID_THRESHOLD) {
            flagSuspiciousActivity(userId, hourBids);
        }

        boolean rateLimited = minuteBids > MAX_BIDS_PER_MINUTE ||
                             hourBids > MAX_BIDS_PER_HOUR ||
                             itemBids > 10; // Max 10 bids per item per hour

        if (rateLimited) {
            log.warn("Rate limit exceeded for user {} - minute: {}, hour: {}, item: {}",
                    userId, minuteBids, hourBids, itemBids);
        }

        return rateLimited;
    }

    public boolean detectShillBidding(String userId, Long itemId, String sellerIp, String bidderIp) {
        // Enhanced IP-based shill bidding detection
        if (sellerIp != null && sellerIp.equals(bidderIp) && !isLocalhost(bidderIp)) {
            log.warn("SHILL BIDDING DETECTED: Same IP for seller and bidder on item {} - IP: {}", itemId, bidderIp);
            flagShillBidding(userId, itemId, bidderIp);
            return true;
        }

        // Check bidding patterns (simplified)
        String patternKey = "bid_pattern:" + userId + ":" + itemId;
        Long consecutiveBids = redisService.incrementValue(patternKey, Duration.ofMinutes(30));

        // Flag if too many consecutive bids
        if (consecutiveBids > MAX_CONSECUTIVE_BIDS) {
            log.warn("Suspicious bidding pattern detected for user {} on item {} - {} consecutive bids",
                    userId, itemId, consecutiveBids);
            return true;
        }

        // Additional pattern detection - rapid bidding from same IP
        String ipPatternKey = "ip_pattern:" + bidderIp + ":" + itemId;
        Long ipBids = redisService.incrementValue(ipPatternKey, Duration.ofMinutes(10));

        if (ipBids > 5) { // More than 5 bids from same IP in 10 minutes
            log.warn("Rapid bidding pattern from IP {} on item {}: {} bids in 10 minutes",
                    bidderIp, itemId, ipBids);
            return true;
        }

        return false;
    }

    private boolean isLocalhost(String ip) {
        return "127.0.0.1".equals(ip) || "::1".equals(ip) || "localhost".equals(ip);
    }

    private void flagSuspiciousActivity(String userId, Long bidCount) {
        log.error("SUSPICIOUS ACTIVITY: User {} has placed {} bids in the last hour", userId, bidCount);

        // Create alert record
        String alertKey = "alert:suspicious:" + userId;
        redisService.setValue(alertKey, "High bid frequency: " + bidCount + " bids/hour", Duration.ofDays(1));

        // Log IP activity
        String userIp = ipAddressService.getCachedUserIp(userId);
        if (userIp != null) {
            ipAddressService.logIpActivity(userId, userIp, "SUSPICIOUS_ACTIVITY");
        }
    }

    private void flagShillBidding(String userId, Long itemId, String ip) {
        log.error("SHILL BIDDING ALERT: User {} bidding on item {} from seller's IP {}", userId, itemId, ip);

        // Create shill bidding alert
        String alertKey = "alert:shill:" + userId + ":" + itemId;
        redisService.setValue(alertKey, "Shill bidding from IP: " + ip, Duration.ofDays(7));

        // Log the incident
        ipAddressService.logIpActivity(userId, ip, "SHILL_BIDDING_DETECTED");
    }
}
