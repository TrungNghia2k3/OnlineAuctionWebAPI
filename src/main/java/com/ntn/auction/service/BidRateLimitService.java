package com.ntn.auction.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BidRateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Configuration constants
    private static final int MAX_BIDS_PER_MINUTE = 5;
    private static final int MAX_BIDS_PER_HOUR = 30;
    private static final int SUSPICIOUS_BID_THRESHOLD = 50; // Bids per hour

    public boolean isRateLimited(String userId, Long itemId) {
        String minuteKey = "bid_rate:minute:" + userId;
        String hourKey = "bid_rate:hour:" + userId;
        String itemKey = "bid_rate:item:" + userId + ":" + itemId;

        // Check minute limit
        Long minuteBids = redisTemplate.opsForValue().increment(minuteKey);
        if (minuteBids == 1) {
            redisTemplate.expire(minuteKey, Duration.ofMinutes(1));
        }

        // Check hour limit
        Long hourBids = redisTemplate.opsForValue().increment(hourKey);
        if (hourBids == 1) {
            redisTemplate.expire(hourKey, Duration.ofHours(1));
        }

        // Check item-specific limit (prevent focus attacks)
        Long itemBids = redisTemplate.opsForValue().increment(itemKey);
        if (itemBids == 1) {
            redisTemplate.expire(itemKey, Duration.ofHours(1));
        }

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
        // Check if bidder and seller have same IP
        if (sellerIp != null && sellerIp.equals(bidderIp)) {
            log.warn("Potential shill bidding detected: same IP for seller and bidder on item {}", itemId);
            return true;
        }

        // Check bidding patterns (simplified)
        String patternKey = "bid_pattern:" + userId + ":" + itemId; // Unique key for user and item
        Long consecutiveBids = redisTemplate.opsForValue().increment(patternKey); // Đếm số lần đặt giá liên tiếp
        redisTemplate.expire(patternKey, Duration.ofMinutes(30)); // Giữ dữ liệu trong 30 phút

        // Cờ nếu có quá nhiều lần đặt giá liên tiếp
        // Tại sao lại là 3? Vì nếu người dùng đặt giá quá nhiều lần liên tiếp, có thể là hành vi đáng ngờ

        // Trong 30 phút, nếu người dùng đặt giá quá 3 lần, có thể là hành vi đáng ngờ
        // Điều này có thể là do người dùng đang cố gắng làm tăng giá một cách không công bằng
        // hoặc đang cố gắng làm giảm giá của người khác bằng cách đặt giá quá nhiều
        // lần liên tiếp.
        if (consecutiveBids > 100) {
            log.warn("Suspicious bidding pattern detected for user {} on item {}", userId, itemId);
            return true;
        }

        return false;
    }

    private void flagSuspiciousActivity(String userId, Long bidCount) {
        log.error("SUSPICIOUS ACTIVITY: User {} has placed {} bids in the last hour", userId, bidCount);
        // Here you could add logic to temporarily suspend the user or require additional verification
    }
}
