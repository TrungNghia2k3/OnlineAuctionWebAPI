package com.ntn.auction.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisService {

    RedisTemplate<String, Object> redisTemplate;

    private static final String BID_LOCK_PREFIX = "bid_lock:";
    private static final String CURRENT_BID_PREFIX = "current_bid:";

    /**
     * Distributed locking operations
     */
    public boolean acquireLock(String lockKey, String lockValue, Duration expiration) {
        try {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, expiration);
            log.debug("Lock acquisition attempt for key {}: {}", lockKey, acquired);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.error("Failed to acquire lock for key {}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    public void releaseLock(String lockKey, String lockValue) {
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                            "return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(RedisScript.of(script, Long.class),
                    Collections.singletonList(lockKey),
                    lockValue
            );
            log.debug("Released lock for key {}", lockKey);
        } catch (Exception e) {
            log.error("Failed to release lock {}: {}", lockKey, e.getMessage());
        }
    }

    public String generateLockKey(Object identifier) {
        return BID_LOCK_PREFIX + identifier;
    }

    public String generateLockValue() {
        return UUID.randomUUID().toString();
    }

    /**
     * Bid caching operations
     */
    public BigDecimal getCurrentBid(Long itemId) {
        try {
            Object cached = redisTemplate.opsForValue().get(CURRENT_BID_PREFIX + itemId);
            return cached != null ? new BigDecimal(cached.toString()) : null;
        } catch (Exception e) {
            log.warn("Failed to get current bid from cache for item {}: {}", itemId, e.getMessage());
            return null;
        }
    }

    public void updateCurrentBid(Long itemId, BigDecimal amount) {
        try {
            redisTemplate.opsForValue().set(
                    CURRENT_BID_PREFIX + itemId,
                    amount.toString(),
                    Duration.ofHours(24)
            );
            log.debug("Updated bid cache for item {} with amount {}", itemId, amount);
        } catch (Exception e) {
            log.warn("Failed to update bid cache for item {}: {}", itemId, e.getMessage());
        }
    }

    public void removeBidCache(Long itemId) {
        try {
            redisTemplate.delete(CURRENT_BID_PREFIX + itemId);
            log.debug("Removed bid cache for item {}", itemId);
        } catch (Exception e) {
            log.warn("Failed to remove bid cache for item {}: {}", itemId, e.getMessage());
        }
    }

    /**
     * Generic cache operations
     */
    public void setValue(String key, Object value, Duration expiration) {
        try {
            redisTemplate.opsForValue().set(key, value, expiration);
        } catch (Exception e) {
            log.error("Failed to set value for key {}: {}", key, e.getMessage());
        }
    }

    public Object getValue(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get value for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    public void deleteKey(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to delete key {}: {}", key, e.getMessage());
        }
    }

    public Long incrementValue(String key, Duration expiration) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value == 1 && expiration != null) {
                redisTemplate.expire(key, expiration);
            }
            return value;
        } catch (Exception e) {
            log.error("Failed to increment value for key {}: {}", key, e.getMessage());
            return 0L;
        }
    }
}
