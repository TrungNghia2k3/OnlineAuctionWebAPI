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
            log.error("Failed to release lock for key {}: {}", lockKey, e.getMessage());
        }
    }

    public String generateLockKey(Long itemId) {
        return BID_LOCK_PREFIX + itemId;
    }

    public String generateLockValue() {
        return UUID.randomUUID().toString();
    }

    /**
     * Bid caching operations
     */
    public BigDecimal getCurrentBid(Long itemId) {
        try {
            Object value = redisTemplate.opsForValue().get(CURRENT_BID_PREFIX + itemId);
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof String) {
                return new BigDecimal((String) value);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get current bid for item {}: {}", itemId, e.getMessage());
            return null;
        }
    }

    public void updateItemCache(com.ntn.auction.entity.Item item) {
        try {
            // Cache current bid price
            redisTemplate.opsForValue().set(
                CURRENT_BID_PREFIX + item.getId(), 
                item.getCurrentBidPrice().toString(),
                Duration.ofHours(24)
            );
            
            // Cache item details
            String itemKey = "item:" + item.getId();
            redisTemplate.opsForHash().put(itemKey, "currentBidPrice", item.getCurrentBidPrice().toString());
            redisTemplate.opsForHash().put(itemKey, "minIncreasePrice", item.getMinIncreasePrice().toString());
            redisTemplate.opsForHash().put(itemKey, "status", item.getStatus().toString());
            redisTemplate.expire(itemKey, Duration.ofHours(24));
            
            log.debug("Updated cache for item {}", item.getId());
        } catch (Exception e) {
            log.error("Failed to update item cache for item {}: {}", item.getId(), e.getMessage());
        }
    }

    /**
     * Generic Redis operations
     */
    public void set(String key, String value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("Failed to set value for key {}: {}", key, e.getMessage());
        }
    }

    public void setWithExpiry(String key, String value, long seconds) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(seconds));
        } catch (Exception e) {
            log.error("Failed to set value with expiry for key {}: {}", key, e.getMessage());
        }
    }

    public String get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("Failed to get value for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    public Integer getInteger(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get integer value for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    public void increment(String key) {
        try {
            redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Failed to increment value for key {}: {}", key, e.getMessage());
        }
    }

    public void setExpiry(String key, long seconds) {
        try {
            redisTemplate.expire(key, Duration.ofSeconds(seconds));
        } catch (Exception e) {
            log.error("Failed to set expiry for key {}: {}", key, e.getMessage());
        }
    }

    public void addToList(String key, String value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
        } catch (Exception e) {
            log.error("Failed to add to list for key {}: {}", key, e.getMessage());
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("Failed to delete key {}: {}", key, e.getMessage());
        }
    }
}
