package com.ntn.auction.service;

import com.ntn.auction.dto.BidInfo;
import com.ntn.auction.entity.Item;
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
    private static final String BID_INFO_PREFIX = "bid_info:";
    private static final String BID_COUNT_PREFIX = "bid_count:";
    private static final String ITEM_CACHE_PREFIX = "item:";
    private static final String ITEM_MIN_INCREMENT_PREFIX = "item_min_increment:";

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

    public void updateItemCache(Item item) {
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

    public Long generateBidId() {
        try {
            String key = "bid_id_generator";
            Long bidId = redisTemplate.opsForValue().increment(key);
            log.debug("Generated new bid ID: {}", bidId);
            return bidId;
        } catch (Exception e) {
            log.error("Failed to generate bid ID: {}", e.getMessage());
            return null;
        }
    }

    public void cacheItem(Item item) {
        try {
            String key = ITEM_CACHE_PREFIX + item.getId();
            redisTemplate.opsForValue().set(key, item, Duration.ofMinutes(30));
            log.debug("Cached item {} for fast access", item.getId());
        } catch (Exception e) {
            log.error("Failed to cache item {}: {}", item.getId(), e.getMessage());
        }
    }

    public Item getCachedItem(Long itemId) {
        try {
            String key = ITEM_CACHE_PREFIX + itemId;
            return (Item) redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Failed to get cached item {}: {}", itemId, e.getMessage());
            return null;
        }
    }

    public void cacheBidInfo(Long bidId, String buyerId, Long itemId, BigDecimal amount) {
        try {
            String key = BID_INFO_PREFIX + bidId;
            BidInfo bidInfo = BidInfo.builder()
                    .bidId(bidId)
                    .buyerId(buyerId)
                    .itemId(itemId)
                    .amount(amount)
                    .timestamp(System.currentTimeMillis())
                    .build();
            redisTemplate.opsForValue().set(key, bidInfo, Duration.ofHours(1));
            log.debug("Cached bid info for bid {}", bidId);
        } catch (Exception e) {
            log.error("Failed to cache bid info for bid {}: {}", bidId, e.getMessage());
        }
    }

    public void updateItemMinIncrement(Long itemId, BigDecimal minIncrement) {
        try {
            String key = ITEM_MIN_INCREMENT_PREFIX + itemId;
            redisTemplate.opsForValue().set(key, minIncrement, Duration.ofHours(2));
            log.debug("Updated min increment for item {} to {}", itemId, minIncrement);
        } catch (Exception e) {
            log.error("Failed to update min increment for item {}: {}", itemId, e.getMessage());
        }
    }

    public void incrementBidCount(String buyerId, Long itemId) {
        try {
            String key = BID_COUNT_PREFIX + buyerId + ":" + itemId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, Duration.ofMinutes(5)); // 5-minute window
        } catch (Exception e) {
            log.error("Failed to increment bid count for user {} on item {}: {}", buyerId, itemId, e.getMessage());
        }
    }

    public void updateBidWithDbId(Long tempBidId, Long dbBidId) {
        try {
            String key = BID_INFO_PREFIX + tempBidId;
            BidInfo bidInfo = (BidInfo) redisTemplate.opsForValue().get(key);
            if (bidInfo != null) {
                bidInfo.setDbBidId(dbBidId);
                redisTemplate.opsForValue().set(key, bidInfo, Duration.ofHours(1));
                log.debug("Updated bid {} with DB ID {}", tempBidId, dbBidId);
            }
        } catch (Exception e) {
            log.error("Failed to update bid {} with DB ID {}: {}", tempBidId, dbBidId, e.getMessage());
        }
    }

    public void revertBidState(Long bidId, Long itemId) {
        try {
            // Get previous bid amount from cache or database
            // This is a simplified revert - in production you might want more sophisticated rollback
            String bidKey = BID_INFO_PREFIX + bidId;
            String currentBidKey = CURRENT_BID_PREFIX + itemId;

            // Remove the failed bid info
            redisTemplate.delete(bidKey);

            // Note: Reverting current bid would require storing previous state
            // For now, we just log the failure and let background job handle cleanup
            log.warn("Reverted Redis state for failed bid {} on item {}", bidId, itemId);

        } catch (Exception e) {
            log.error("Failed to revert Redis state for bid {} on item {}: {}", bidId, itemId, e.getMessage());
        }
    }

    public void setCurrentBid(Long itemId, BigDecimal amount) {
        try {
            redisTemplate.opsForValue().set(
                CURRENT_BID_PREFIX + itemId,
                amount.toString(),
                Duration.ofHours(24)
            );
            log.debug("Set current bid for item {} to {}", itemId, amount);
        } catch (Exception e) {
            log.error("Failed to set current bid for item {}: {}", itemId, e.getMessage());
        }
    }
}
