package com.ntn.auction.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BidService {

    // Repositories
    BidRepository bidRepository;
    ItemRepository itemRepository;
    UserRepository userRepository;

    // Mappers
    BidMapper bidMapper;

    // Focused services following SRP
    BidAuditService bidAuditService;
    BidRateLimitService bidRateLimitService;
    ProxyBidService proxyBidService;
    RedisService redisService;
    WebSocketService webSocketService;
    IpAddressService ipAddressService;

    /**
     * Core bid placement method - orchestrates the entire bidding process
     */
    @Transactional
    public BidResponse placeBid(CreateBidRequest createBidRequest) {
        String lockKey = redisService.generateLockKey(createBidRequest.getItemId());
        String lockValue = redisService.generateLockValue();
        String ipAddress = ipAddressService.getClientIpAddress();

        log.info("Processing bid - User: {}, Item: {}, Amount: {}", createBidRequest.getBuyerId(), createBidRequest.getItemId(), createBidRequest.getAmount());

        try {
            // 1. Rate limiting check
            if (bidRateLimitService.isRateLimited(createBidRequest.getBuyerId(), createBidRequest.getItemId())) {
                throw new BidException("Rate limit exceeded. Please wait before placing another bid.");
            }

            // 2. Acquire distributed lock
            if (!redisService.acquireLock(lockKey, lockValue, Duration.ofSeconds(10))) {
                throw new BidException("Another bid is being processed. Please try again.");
            }

            // 3. Fetch and validate entities
            User currentUser = userRepository.findById(createBidRequest.getBuyerId())
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            Item item = itemRepository.findById(createBidRequest.getItemId())
                    .orElseThrow(() -> new ItemNotFoundException("Item not found"));

            // 4. Cache user IP for future fraud detection
            ipAddressService.cacheUserIp(createBidRequest.getBuyerId(), ipAddress);

            // 5. Shill bidding detection with real IPs
            String sellerIp = ipAddressService.getSellerIpAddress(item.getSeller().getId());
            if (bidRateLimitService.detectShillBidding(createBidRequest.getBuyerId(), createBidRequest.getItemId(), sellerIp, ipAddress)) {
                throw new BidException("Suspicious bidding pattern detected");
            }

            // 6. Enhanced IP-based fraud detection
            if (ipAddressService.isSameIpAddress(createBidRequest.getBuyerId(), item.getSeller().getId())) {
                throw new BidException("Bidding from same IP as seller is not allowed");
            }

            // 7. Business validation
            validateBidRules(item, currentUser, createBidRequest.getAmount());

            // 8. Cache validation
            BigDecimal cachedCurrentBid = redisService.getCurrentBid(createBidRequest.getItemId());
            if (cachedCurrentBid != null && createBidRequest.getAmount().compareTo(cachedCurrentBid) <= 0) {
                throw new BidException("Bid amount must be higher than current bid: " + cachedCurrentBid);
            }

            // 9. Create and save bid
            Bid savedBid = createAndSaveBid(item, currentUser, createBidRequest.getAmount());

            // 10. Update item and cache
            updateItemAndCache(item, createBidRequest.getAmount());

            // 11. Audit logging with real IP
            bidAuditService.logBidAction(savedBid, BidAuditLog.ActionType.BID_PLACED, ipAddress);

            // 12. Log IP activity for audit
            ipAddressService.logIpActivity(createBidRequest.getBuyerId(), ipAddress, "BID_PLACED");

            // 13. Process proxy bids
            proxyBidService.processProxyBidsAfterManualBid(item, createBidRequest.getAmount(), currentUser);

            // 14. Real-time notifications
            Long totalBids = bidRepository.countByItemId(item.getId());
            webSocketService.sendBidUpdate(savedBid, item, totalBids);

            log.info("Bid placed successfully - ID: {}, Amount: {}", savedBid.getId(), savedBid.getAmount());
            return bidMapper.toResponse(savedBid);

        } finally {
            redisService.releaseLock(lockKey, lockValue);
        }
    }

    /**
     * Get all bids for an item
     */
    public List<BidResponse> getItemBids(Long itemId) {
        List<Bid> bids = bidRepository.findByItemIdOrderByAmountDesc(itemId);
        return bidMapper.toResponseList(bids);
    }

    /**
     * Update bid statuses at auction end
     */
    @Transactional
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

    /**
     * Private helper methods
     */
    private Bid createAndSaveBid(Item item, User buyer, BigDecimal amount) {
        // Reset previous highest bid flags
        bidRepository.resetHighestBidFlags(item.getId());
        bidRepository.markPreviousBidsAsOutbid(item.getId());

        Bid newBid = Bid.builder()
                .item(item)
                .buyer(buyer)
                .amount(amount)
                .bidTime(LocalDateTime.now())
                .status(Bid.BidStatus.ACCEPTED)
                .highestBid(true)
                .proxyBid(false)
                .build();

        return bidRepository.save(newBid);
    }

    private void updateItemAndCache(Item item, BigDecimal amount) {
        item.setCurrentBidPrice(amount);
        itemRepository.save(item);
        redisService.updateCurrentBid(item.getId(), amount);
    }

    private void validateBidRules(Item item, User user, BigDecimal bidAmount) {
        LocalDateTime now = LocalDateTime.now();

        // Time validation
        if (now.isBefore(item.getAuctionStartDate())) {
            throw new BidException("Auction has not started yet");
        }
        if (now.isAfter(item.getAuctionEndDate())) {
            throw new BidException("Auction has ended");
        }

        // Status validation
        if (item.getStatus() != Item.ItemStatus.ACTIVE) {
            throw new BidException("Item is not available for bidding");
        }

        // Seller validation
        if (item.getSeller().getId().equals(user.getId())) {
            throw new BidException("Sellers cannot bid on their own items");
        }

        // Amount validation
        BigDecimal currentPrice = item.getCurrentBidPrice() != null ?
                item.getCurrentBidPrice() : item.getStartingPrice();
        BigDecimal minimumBid = currentPrice.add(item.getMinIncreasePrice());

        if (bidAmount.compareTo(minimumBid) < 0) {
            throw new BidException("Bid must be at least " + minimumBid);
        }

        // Reserve price logging (don't throw exception)
        if (item.getReservePrice() != null && bidAmount.compareTo(item.getReservePrice()) < 0) {
            log.info("Bid {} is below reserve price for item {}", bidAmount, item.getId());
        }
    }
}
