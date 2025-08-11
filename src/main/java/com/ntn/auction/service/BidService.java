package com.ntn.auction.service;

import com.ntn.auction.dto.event.BidUpdateEvent;
import com.ntn.auction.dto.request.BidUpdateRequest;
import com.ntn.auction.dto.request.CreateBidRequest;
import com.ntn.auction.dto.response.BidResponse;
import com.ntn.auction.entity.Bid;
import com.ntn.auction.entity.BidAuditLog;
import com.ntn.auction.entity.Item;
import com.ntn.auction.entity.User;
import com.ntn.auction.exception.BidException;
import com.ntn.auction.exception.ItemNotFoundException;
import com.ntn.auction.exception.UserNotFoundException;
import com.ntn.auction.mapper.BidMapper;
import com.ntn.auction.repository.BidRepository;
import com.ntn.auction.repository.ItemRepository;
import com.ntn.auction.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BidService {

    BidRepository bidRepository;
    ItemRepository itemRepository;
    UserRepository userRepository;
    BidMapper bidMapper;

    // Enhanced with new services
    BidAuditService bidAuditService;
    BidRateLimitService bidRateLimitService;
    ProxyBidService proxyBidService;

    // Existing WebSocket and Redis components
    SimpMessagingTemplate messagingTemplate;
    RedisTemplate<String, Object> redisTemplate;

    private static final String BID_LOCK_PREFIX = "bid_lock:";
    private static final String CURRENT_BID_PREFIX = "current_bid:";

    /**
     * Enhanced placeBid method that integrates all services while preserving WebSocket and Redis functionality
     */
    @Transactional
    public BidResponse placeBid(CreateBidRequest createBidRequest) {
        String lockKey = BID_LOCK_PREFIX + createBidRequest.getItemId();
        String lockValue = UUID.randomUUID().toString();
        String ipAddress = getClientIpAddress(); // You'll need to implement this

        log.info("Enhanced bid processing - User: {}, Item: {}, Amount: {}",
                createBidRequest.getBuyerId(), createBidRequest.getItemId(), createBidRequest.getAmount());

        try {
            // 1. ENHANCED: Rate limiting check using new service
            if (bidRateLimitService.isRateLimited(createBidRequest.getBuyerId(), createBidRequest.getItemId())) {
                throw new BidException("Rate limit exceeded. Please wait before placing another bid.");
            }

            // 2. Distributed locking (Redis) - PRESERVED
            Boolean lockAcquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, Duration.ofSeconds(10));

            if (!Boolean.TRUE.equals(lockAcquired)) {
                throw new BidException("Another bid is being processed. Please try again.");
            }

            // 3. Fetch entities with proper error handling
            User currentUser = userRepository.findById(createBidRequest.getBuyerId())
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            Item item = itemRepository.findById(createBidRequest.getItemId())
                    .orElseThrow(() -> new ItemNotFoundException("Item not found"));

            // 4. ENHANCED: Shill bidding detection using new service
            String sellerIp = getSellerIpAddress();
            if (bidRateLimitService.detectShillBidding(createBidRequest.getBuyerId(), createBidRequest.getItemId(), sellerIp, ipAddress)) {
                throw new BidException("Suspicious bidding pattern detected");
            }

            // 5. Business validation (enhanced)
            validateBidRules(item, currentUser, createBidRequest.getAmount());

            // 6. Get current highest bid from cache (Redis) - PRESERVED
            BigDecimal cachedCurrentBid = getCurrentBidFromCache(createBidRequest.getItemId());
            if (cachedCurrentBid != null && createBidRequest.getAmount().compareTo(cachedCurrentBid) <= 0) {
                throw new BidException("Bid amount must be higher than current bid: " + cachedCurrentBid);
            }

            // 7. Update previous bids
            bidRepository.resetHighestBidFlags(createBidRequest.getItemId());
            bidRepository.markPreviousBidsAsOutbid(createBidRequest.getItemId());

            // 8. Create and save new bid
            Bid newBid = Bid.builder()
                    .item(item)
                    .buyer(currentUser)
                    .amount(createBidRequest.getAmount())
                    .bidTime(LocalDateTime.now())
                    .status(Bid.BidStatus.ACCEPTED)
                    .highestBid(true)
                    .proxyBid(false)
                    .build();

            Bid savedBid = bidRepository.save(newBid);

            // 9. Update item current price
            item.setCurrentBidPrice(createBidRequest.getAmount());
            itemRepository.save(item);

            // 10. Update Redis cache - PRESERVED
            updateCurrentBidCache(createBidRequest.getItemId(), createBidRequest.getAmount());

            // 11. ENHANCED: Create audit log using new service
            bidAuditService.logBidAction(savedBid, BidAuditLog.ActionType.BID_PLACED, ipAddress);

            // 12. ENHANCED: Process proxy bids using new service
            proxyBidService.processProxyBidsAfterManualBid(item, createBidRequest.getAmount(), currentUser);

            // 13. Create response
            BidResponse bidResponse = bidMapper.toResponse(savedBid);

            // 14. WebSocket broadcast - PRESERVED
            broadcastBidUpdate(savedBid, item);

            log.info("Enhanced bid placed successfully - ID: {}, Amount: {}", savedBid.getId(), savedBid.getAmount());
            return bidResponse;

        } finally {
            // 15. Release distributed lock - PRESERVED
            releaseLock(lockKey, lockValue);
            log.debug("Released lock for key {}", lockKey);
        }
    }

    public List<BidResponse> getItemBids(Long itemId) {
        List<Bid> bids = bidRepository.findByItemIdOrderByAmountDesc(itemId);
        return bidMapper.toResponseList(bids);
    }

    public void updateBidsStatus(Item item, Bid winningBid) {
        List<Bid> allBids = bidRepository.findByItem(item);

        for (Bid bid : allBids) {
            BidUpdateRequest updateRequest;

            if (bid.getId().equals(winningBid.getId())) {
                updateRequest = BidUpdateRequest.builder()
                        .status(Bid.BidStatus.WON)
                        .highestBid(true)
                        .build();
            } else {
                updateRequest = BidUpdateRequest.builder()
                        .status(Bid.BidStatus.LOST)
                        .highestBid(false)
                        .build();
            }

            bidMapper.updateFromRequest(updateRequest, bid);
        }

        bidRepository.saveAll(allBids);
    }

    // Helper methods
    private String getClientIpAddress() {
        // Implementation to get client IP from request context
        return "127.0.0.1"; // Placeholder
    }

    private String getSellerIpAddress() {
        // Implementation to get seller's IP from session/cache
        return "192.168.1.1"; // Placeholder
    }

    /**
     * Enhanced business validation with better error messages
     */
    private void validateBidRules(Item item, User user, BigDecimal bidAmount) {
        // Check if auction is active
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getAuctionStartDate())) {
            throw new BidException("Auction has not started yet");
        }
        if (now.isAfter(item.getAuctionEndDate())) {
            throw new BidException("Auction has ended");
        }

        // Check item status
        if (item.getStatus() != Item.ItemStatus.ACTIVE) {
            throw new BidException("Item is not available for bidding");
        }

        // Check if user is not the seller
        if (item.getSeller().getId().equals(user.getId())) {
            throw new BidException("Sellers cannot bid on their own items");
        }

        // Kiểm tra số tiền đặt cọc
        // Nếu không có giá thầu hiện tại, sử dụng giá khởi điểm
        // Nếu có giá thầu hiện tại, sử dụng giá thầu hiện tại
        BigDecimal currentPrice = item.getCurrentBidPrice() != null ?
                item.getCurrentBidPrice() : item.getStartingPrice();

        BigDecimal minimumBid = currentPrice.add(item.getMinIncreasePrice());
        if (bidAmount.compareTo(minimumBid) < 0) {
            throw new BidException("Bid must be at least " + minimumBid);
        }

        // Check reserve price if set
        if (item.getReservePrice() != null && bidAmount.compareTo(item.getReservePrice()) < 0) {
            log.info("Bid {} is below reserve price for item {}", bidAmount, item.getId());
            // Don't throw exception, just log - reserve price is usually hidden
        }
    }

    /**
     * WebSocket broadcast - PRESERVED and enhanced
     */
    public void broadcastBidUpdate(Bid bid, Item item) {
        try {
            BidUpdateEvent bidUpdateEvent = BidUpdateEvent.builder()
                    .bidId(bid.getId())
                    .itemId(item.getId())
                    .amount(bid.getAmount())
                    .buyerName(bid.getBuyer().getFirstName() + " " + bid.getBuyer().getLastName())
                    .buyerId(bid.getBuyer().getId())
                    .bidTime(bid.getBidTime())
                    .status(bid.getStatus())
                    .totalBids(bidRepository.countByItemId(item.getId()))
                    .build();

            // Broadcast to item-specific topic
            messagingTemplate.convertAndSend("/topic/item/" + item.getId() + "/bids", bidUpdateEvent);

            // Broadcast to general auction updates
            messagingTemplate.convertAndSend("/topic/auctions/updates", bidUpdateEvent);

            log.debug("WebSocket bid update broadcasted for item {}", item.getId());

        } catch (Exception e) {
            log.error("Failed to broadcast bid update via WebSocket: {}", e.getMessage());
            // Don't throw exception - WebSocket failure shouldn't break bid processing
        }
    }


    public BigDecimal getCurrentBidFromCache(Long itemId) {
        try {
            Object cached = redisTemplate.opsForValue().get(CURRENT_BID_PREFIX + itemId);
            return cached != null ? new BigDecimal(cached.toString()) : null;
        } catch (Exception e) {
            log.warn("Failed to get current bid from cache for item {}: {}", itemId, e.getMessage());
            return null;
        }
    }

    public void updateCurrentBidCache(Long itemId, BigDecimal amount) {
        try {
            redisTemplate.opsForValue().set(CURRENT_BID_PREFIX + itemId, amount.toString(), Duration.ofHours(24));
            log.debug("Updated bid cache for item {} with amount {}", itemId, amount);
        } catch (Exception e) {
            log.warn("Failed to update bid cache for item {}: {}", itemId, e.getMessage());
        }
    }

    public void releaseLock(String lockKey, String lockValue) {
        try {
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                            "return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                    org.springframework.data.redis.core.script.RedisScript.of(script, Long.class),
                    java.util.Collections.singletonList(lockKey),
                    lockValue
            );
        } catch (Exception e) {
            log.error("Failed to release lock {}: {}", lockKey, e.getMessage());
        }
    }
}
