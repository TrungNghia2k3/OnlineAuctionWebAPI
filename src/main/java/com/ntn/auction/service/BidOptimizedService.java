package com.ntn.auction.service;

import com.ntn.auction.dto.BidNotificationPayload;
import com.ntn.auction.dto.event.BidProcessingEvent;
import com.ntn.auction.dto.BidValidationResult;
import com.ntn.auction.dto.request.BidCreateRequest;
import com.ntn.auction.dto.response.BidResponse;
import com.ntn.auction.entity.Bid;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Optimized Bid Service for real-time performance
 * Separates critical path (Redis + immediate response) from heavy processing (DB + audit)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BidOptimizedService {

    // Core repositories - minimal usage in critical path
    BidRepository bidRepository;
    ItemRepository itemRepository;
    UserRepository userRepository;
    BidMapper bidMapper;

    // Focused services following SRP
    BidAuditService bidAuditService;
    BidRateLimitService bidRateLimitService;
    ProxyBidService proxyBidService;
    RedisService redisService;
    WebSocketService webSocketService;
    IpAddressService ipAddressService;
    BidIncrementService bidIncrementService;
    ItemService itemService;

    // Event publisher for async processing
    ApplicationEventPublisher eventPublisher;

    // OPTIMIZED BID PLACEMENT - Fast Critical Path
    // Phase 1: Redis-based validation + immediate response
    // Phase 2: Async DB persistence + heavy processing

    public BidResponse placeBidOptimized(BidCreateRequest bidCreateRequest) {
        String lockKey = redisService.generateLockKey(bidCreateRequest.getItemId());
        String lockValue = redisService.generateLockValue();
        String ipAddress = ipAddressService.getClientIpAddress();

        log.info("Processing optimized bid - User: {}, Item: {}, Amount: {}",
                bidCreateRequest.getBuyerId(), bidCreateRequest.getItemId(), bidCreateRequest.getAmount());

        try {
            // ===== PHASE 1: FAST CRITICAL PATH (Redis-only) =====

            // 1. Acquire distributed lock - FAST (Redis operation ~1ms)
            if (!redisService.acquireLock(lockKey, lockValue, Duration.ofSeconds(5))) {
                throw new BidException("Another bid is being processed. Please try again.");
            }

            // 2. Fast validation using Redis cache - FAST (~1-2ms total)
            BidValidationResult validation = performFastValidation(bidCreateRequest, ipAddress);

            // 3. Update Redis immediately - FAST (~1ms)
            Long bidId = updateRedisState(bidCreateRequest, validation.getItem());

            // 4. Send real-time notification immediately - ASYNC (~1ms to queue)
            sendImmediateNotification(bidCreateRequest, validation.getItem(), bidId);

            // 5. Create immediate response - FAST
            BidResponse immediateResponse = createImmediateResponse(bidId, bidCreateRequest, validation.getItem());

            // ===== PHASE 2: ASYNC HEAVY PROCESSING =====

            // 6. Publish event for background processing - ASYNC (~1ms to queue)
            publishBidProcessingEvent(bidCreateRequest, validation, bidId, ipAddress);

            log.info("Bid placed successfully (immediate) - ID: {}, Amount: {}",
                    bidId, bidCreateRequest.getAmount());

            return immediateResponse;

        } finally {
            redisService.releaseLock(lockKey, lockValue);
        }
    }

    @Transactional
    public void processBidInBackground(BidProcessingEvent event) {
        try {
            log.info("Starting background processing for bid {}", event.getBidId());

            // 1. Load entities from DB
            User buyer = userRepository.findById(event.getBidCreateRequest().getBuyerId())
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            Item item = itemRepository.findById(event.getBidCreateRequest().getItemId())
                    .orElseThrow(() -> new ItemNotFoundException("Item not found"));

            // 2. Perform heavy fraud detection
            performComprehensiveFraudDetection(event, item);

            // 3. Create and persist bid in DB
            Bid persistedBid = createAndPersistBid(event, item, buyer);

            // 4. Update item in DB
            updateItemInDatabase(item, event.getBidCreateRequest().getAmount());

            // 5. Update minimum increase price in DB
            updateMinIncreasePriceInDb(item.getId());

            // 6. Process proxy bids
            processProxyBidsAsync(item, event.getBidCreateRequest().getAmount(), buyer);

            // 7. Audit logging
            performAuditLogging(persistedBid, event.getIpAddress());

            // 8. Update Redis with final DB ID
            redisService.updateBidWithDbId(event.getBidId(), persistedBid.getId());

            // 9. Send completion notification
            sendCompletionNotification(persistedBid, item);

            log.info("Completed background processing for bid {}", event.getBidId());

        } catch (Exception e) {
            log.error("Error in background bid processing for bid {}: {}", event.getBidId(), e.getMessage(), e);
            handleBackgroundProcessingError(event, e);
        }
    }

    // ===== FAST VALIDATION METHODS =====

    private BidValidationResult performFastValidation(BidCreateRequest request, String ipAddress) {
        // Get cached item data
        Item cachedItem = redisService.getCachedItem(request.getItemId());
        if (cachedItem == null) {
            // Fallback to DB for cache miss - but load into cache immediately
            cachedItem = itemRepository.findById(request.getItemId())
                    .orElseThrow(() -> new ItemNotFoundException("Item not found"));
            redisService.cacheItem(cachedItem);
        }

        // Fast rate limiting check using Redis counters
        if (bidRateLimitService.isRateLimitedFast(request.getBuyerId(), request.getItemId())) {
            throw new BidException("Rate limit exceeded. Please wait before placing another bid.");
        }

        // Fast auction timing validation
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(cachedItem.getAuctionStartDate()) || now.isAfter(cachedItem.getAuctionEndDate())) {
            throw new BidException("Auction is not active");
        }

        // Fast price validation using cached current bid
        BigDecimal cachedCurrentBid = redisService.getCurrentBid(request.getItemId());

        // Fallback to database current bid if Redis cache is empty (e.g., after server restart)
        if (cachedCurrentBid == null) {
            cachedCurrentBid = cachedItem.getCurrentBidPrice() != null ?
                cachedItem.getCurrentBidPrice() : cachedItem.getStartingPrice();
            // Update Redis cache with current value for future requests
            redisService.updateItemCache(cachedItem);
        }

        BigDecimal minimumBid = cachedCurrentBid.add(cachedItem.getMinIncreasePrice());

        if (request.getAmount().compareTo(minimumBid) < 0) {
            throw new BidException("Bid must be at least " + minimumBid);
        }

        // Cache user IP for fraud detection (async processing will handle full fraud check)
        ipAddressService.cacheUserIp(request.getBuyerId(), ipAddress);

        return BidValidationResult.builder()
                .item(cachedItem)
                .currentBid(cachedCurrentBid)
                .minimumBid(minimumBid)
                .build();
    }

    private Long updateRedisState(BidCreateRequest request, Item item) {
        Long bidId = redisService.generateBidId();

        // Update current bid price in Redis
        redisService.setCurrentBid(request.getItemId(), request.getAmount());

        // Update minimum increase price based on new amount
        BigDecimal newMinIncrement = bidIncrementService.calculateMinIncrement(request.getAmount());
        redisService.updateItemMinIncrement(request.getItemId(), newMinIncrement);

        // Cache bid info for immediate reference
        redisService.cacheBidInfo(bidId, request.getBuyerId(), request.getItemId(), request.getAmount());

        // Update bid count for rate limiting
        redisService.incrementBidCount(request.getBuyerId(), request.getItemId());

        log.debug("Updated Redis state for bid {} - new price: {}, new increment: {}",
                bidId, request.getAmount(), newMinIncrement);

        return bidId;
    }

    private void sendImmediateNotification(BidCreateRequest request, Item item, Long bidId) {
        try {
            // Create lightweight notification payload
            BidNotificationPayload payload = BidNotificationPayload.builder()
                    .bidId(bidId)
                    .itemId(request.getItemId())
                    .buyerId(request.getBuyerId())
                    .amount(request.getAmount())
                    .timestamp(LocalDateTime.now())
                    .build();

            // Send async notification (doesn't block critical path)
            webSocketService.sendBidUpdateAsync(payload);

        } catch (Exception e) {
            // Log but don't fail the bid for notification errors
            log.error("Failed to send immediate notification for bid {}: {}", bidId, e.getMessage());
        }
    }

    private BidResponse createImmediateResponse(Long bidId, BidCreateRequest request, Item item) {
        return BidResponse.builder()
                .id(bidId) // Temporary ID until DB persistence
                .itemId(request.getItemId())
                .buyerId(request.getBuyerId())
                .amount(request.getAmount())
                .bidTime(LocalDateTime.now())
                .status(Bid.BidStatus.ACCEPTED) // Temporary status
                .highestBid(true)
                .proxyBid(false)
                .build();
    }

    private void publishBidProcessingEvent(BidCreateRequest request, BidValidationResult validation, Long bidId, String ipAddress) {
        BidProcessingEvent event = BidProcessingEvent.builder()
                .bidId(bidId)
                .bidCreateRequest(request)
                .validationResult(validation)
                .ipAddress(ipAddress)
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publishEvent(event);
        log.debug("Published bid processing event for bid {}", bidId);
    }

    // ===== HELPER METHODS FOR BACKGROUND PROCESSING =====

    private void performComprehensiveFraudDetection(BidProcessingEvent event, Item item) {
        // Heavy fraud detection that was removed from critical path
        String sellerIp = ipAddressService.getSellerIpAddress(item.getSeller().getId());
        if (bidRateLimitService.detectShillBidding(
                event.getBidCreateRequest().getBuyerId(),
                event.getBidCreateRequest().getItemId(),
                sellerIp,
                event.getIpAddress())) {
            throw new BidException("Suspicious bidding pattern detected");
        }

        if (ipAddressService.isSameIpAddress(event.getBidCreateRequest().getBuyerId(), item.getSeller().getId())) {
            throw new BidException("Bidding from same IP as seller is not allowed");
        }
    }

    private Bid createAndPersistBid(BidProcessingEvent event, Item item, User buyer) {
        // Reset previous highest bid flags
        bidRepository.resetHighestBidFlags(item.getId());
        bidRepository.markPreviousBidsAsOutbid(item.getId());

        Bid newBid = Bid.builder()
                .item(item)
                .buyer(buyer)
                .amount(event.getBidCreateRequest().getAmount())
                .bidTime(LocalDateTime.now())
                .status(Bid.BidStatus.ACCEPTED)
                .highestBid(true)
                .proxyBid(false)
                .build();

        return bidRepository.save(newBid);
    }

    private void updateItemInDatabase(Item item, BigDecimal newBidAmount) {
        item.setCurrentBidPrice(newBidAmount);
        BigDecimal newMinIncreasePrice = bidIncrementService.calculateMinIncrement(newBidAmount);
        item.setMinIncreasePrice(newMinIncreasePrice);
        itemRepository.save(item);
    }

    private void updateMinIncreasePriceInDb(Long itemId) {
        // This is your existing updateMinIncreasePrice method
        // Moved to background processing
        try {
            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new ItemNotFoundException("Item not found with ID: " + itemId));

            BigDecimal newMinIncreasePrice = bidIncrementService.calculateMinIncrement(item.getCurrentBidPrice());

            if (!newMinIncreasePrice.equals(item.getMinIncreasePrice())) {
                item.setMinIncreasePrice(newMinIncreasePrice);
                itemRepository.save(item);
                redisService.updateItemCache(item);
            }
        } catch (Exception e) {
            log.error("Error updating min increase price for item {}: {}", itemId, e.getMessage());
        }
    }

    private void processProxyBidsAsync(Item item, BigDecimal newBidAmount, User excludeUser) {
        // Your existing proxy bid processing - now async
        try {
            proxyBidService.processProxyBidsAfterManualBid(item, newBidAmount, excludeUser);
        } catch (Exception e) {
            log.error("Error processing proxy bids for item {}: {}", item.getId(), e.getMessage());
        }
    }

    private void performAuditLogging(Bid bid, String ipAddress) {
        // Your existing audit logging - now async
        try {
            bidAuditService.logBidAction(bid, com.ntn.auction.entity.BidAuditLog.ActionType.BID_PLACED, ipAddress);
            ipAddressService.logIpActivity(bid.getBuyer().getId(), ipAddress, "BID_PLACED");
        } catch (Exception e) {
            log.error("Error in audit logging for bid {}: {}", bid.getId(), e.getMessage());
        }
    }

    private void sendCompletionNotification(Bid bid, Item item) {
        try {
            Long totalBids = bidRepository.countByItemId(item.getId());
            webSocketService.sendBidCompletionNotification(bid, item, totalBids);
        } catch (Exception e) {
            log.error("Error sending completion notification for bid {}: {}", bid.getId(), e.getMessage());
        }
    }

    private void handleBackgroundProcessingError(BidProcessingEvent event, Exception e) {
        // Handle background processing failures
        // Could implement retry logic, compensation, or alerting
        log.error("Background processing failed for bid {}, implementing compensation", event.getBidId());

        // Revert Redis state if DB processing failed
        try {
            redisService.revertBidState(event.getBidId(), event.getBidCreateRequest().getItemId());
        } catch (Exception revertError) {
            log.error("Failed to revert Redis state for failed bid {}: {}", event.getBidId(), revertError.getMessage());
        }
    }
}
