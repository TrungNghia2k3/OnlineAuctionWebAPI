package com.ntn.auction.service;

import com.ntn.auction.entity.Bid;
import com.ntn.auction.entity.Item;
import com.ntn.auction.entity.ProxyBid;
import com.ntn.auction.entity.User;
import com.ntn.auction.exception.ItemNotFoundException;
import com.ntn.auction.exception.UserNotFoundException;
import com.ntn.auction.repository.BidRepository;
import com.ntn.auction.repository.ItemRepository;
import com.ntn.auction.repository.ProxyBidRepository;
import com.ntn.auction.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProxyBidService {

    ProxyBidRepository proxyBidRepository;
    BidRepository bidRepository;
    ItemRepository itemRepository;
    UserRepository userRepository;
    RedisService redisService;
    WebSocketService webSocketService;

    /**
     * Process proxy bids after a manual bid is placed
     * Handles proxy bid competition and status updates
     *
     * @param item         the auction item
     * @param newBidAmount the new manual bid amount
     * @param excludeUser  the user who placed the manual bid (to exclude from proxy processing)
     */
    @Transactional
    public void processProxyBidsAfterManualBid(Item item, BigDecimal newBidAmount, User excludeUser) {
        log.info("Processing proxy bids for item {} after manual bid of {}", item.getId(), newBidAmount);

        // Step 1: Get all active proxy bids for this item
        List<ProxyBid> allActiveProxyBids = getAllActiveProxyBidsExcludingUser(item.getId(), excludeUser.getId());

        // Step 2: Categorize proxy bids into eligible and exceeded
        ProxyBidCategories categories = categorizeProxyBids(allActiveProxyBids, newBidAmount);

        // Step 3: Update exceeded proxy bids status
        updateExceededProxyBids(categories.exceededProxyBids(), newBidAmount);

        // Step 4: Process eligible proxy bids if any exist
        if (categories.eligibleProxyBids().isEmpty()) {
            log.info("No eligible proxy bids remaining for item {} after manual bid {}", item.getId(), newBidAmount);
            return;
        }

        // Step 5: Execute highest eligible proxy bid
        executeHighestEligibleProxyBid(item, newBidAmount, categories.eligibleProxyBids());
    }

    /**
     * Create or update a proxy bid with immediate execution logic
     * Implements business rules for same-user vs different-user proxy bids
     *
     * @param userId    the user creating/updating the proxy bid
     * @param itemId    the auction item ID
     * @param maxAmount the maximum amount for proxy bidding
     * @return the created/updated ProxyBid
     */
    @Transactional
    public ProxyBid createOrUpdateProxyBid(String userId, Long itemId, BigDecimal maxAmount) {
        log.info("Creating/updating proxy bid: User {} - Item {} - Max Amount {}", userId, itemId, maxAmount);

        // Step 1: Validate request and fetch entities
        ProxyBidContext context = validateAndPrepareProxyBid(userId, itemId, maxAmount);

        // Step 2: Create or update the proxy bid record
        ProxyBid proxyBid = createOrUpdateProxyBidRecord(context);

        // Step 3: Determine execution strategy based on current highest bidder
        ExecutionStrategy strategy = determineExecutionStrategy(context.item(), userId);

        // Step 4: Execute proxy bid if conditions are met
        if (strategy.shouldExecute()) {
            executeProxyBidIfAffordable(proxyBid, context.item(), context.minimumRequired());
        }

        return proxyBid;
    }

    public List<ProxyBid> getUserProxyBids(String userId) {
        return proxyBidRepository.findByUserId(userId);
    }

    public List<ProxyBid> getActiveProxyBidsForItem(Long itemId) {
        return proxyBidRepository.findActiveProxyBidsForItem(itemId);
    }

    @Transactional
    public void cancelProxyBid(Long proxyBidId, String userId) {
        ProxyBid proxyBid = proxyBidRepository.findById(proxyBidId)
                .orElseThrow(() -> new RuntimeException("Proxy bid not found: " + proxyBidId));

        // Validate ownership
        if (!proxyBid.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("User can only cancel their own proxy bids");
        }

        proxyBid.setStatus(ProxyBid.ProxyBidStatus.CANCELLED);
        proxyBid.setWinning(false);
        proxyBidRepository.save(proxyBid);

        log.info("Cancelled proxy bid {} for user {}", proxyBidId, userId);
    }

    public List<ProxyBid> getWinningProxyBidsByUser(String userId) {
        return proxyBidRepository.findWinningProxyBidsByUser(userId);
    }

    @Transactional
    public void updateProxyBidsAtAuctionEnd(Item item, Bid winningBid) {
        List<ProxyBid> itemProxyBids = proxyBidRepository.findActiveProxyBidsForItem(item.getId());

        for (ProxyBid proxyBid : itemProxyBids) {
            if (winningBid != null && winningBid.getBuyer().getId().equals(proxyBid.getUser().getId())) {
                proxyBid.setStatus(ProxyBid.ProxyBidStatus.WON);
                proxyBid.setWinning(true);
            } else {
                proxyBid.setStatus(ProxyBid.ProxyBidStatus.OUTBID);
                proxyBid.setWinning(false);
            }
        }

        proxyBidRepository.saveAll(itemProxyBids);
        log.info("Updated {} proxy bid statuses for completed auction item {}", itemProxyBids.size(), item.getId());
    }

    private List<ProxyBid> getAllActiveProxyBidsExcludingUser(Long itemId, String excludeUserId) {
        // Get ALL active proxy bids for this item
        List<ProxyBid> allActiveProxyBids = proxyBidRepository.findActiveProxyBidsForItem(itemId);

        // Remove proxy bids from the manual bidder to avoid self-bidding
        allActiveProxyBids.removeIf(pb -> pb.getUser().getId().equals(excludeUserId));

        return allActiveProxyBids;
    }

    private ProxyBidCategories categorizeProxyBids(List<ProxyBid> allActiveProxyBids, BigDecimal newBidAmount) {
        // Process ALL proxy bids - separate into eligible and exceeded
        List<ProxyBid> eligibleProxyBids = allActiveProxyBids.stream()
                .filter(pb -> pb.getMaxAmount().compareTo(newBidAmount) > 0)
                .toList();

        List<ProxyBid> exceededProxyBids = allActiveProxyBids.stream()
                .filter(pb -> pb.getMaxAmount().compareTo(newBidAmount) <= 0)
                .toList();

        return new ProxyBidCategories(eligibleProxyBids, exceededProxyBids);
    }

    private void updateExceededProxyBids(List<ProxyBid> exceededProxyBids, BigDecimal newBidAmount) {
        // FIXED: Always update exceeded proxy bids to OUTBID status
        // This ensures proxy bids are properly updated even when eligibleProxyBids is empty
        for (ProxyBid exceededBid : exceededProxyBids) {
            exceededBid.setStatus(ProxyBid.ProxyBidStatus.OUTBID);
            exceededBid.setWinning(false);
            proxyBidRepository.save(exceededBid);
            log.info("Proxy bid {} exceeded by manual bid amount {} - status updated to OUTBID",
                    exceededBid.getId(), newBidAmount);
        }
    }

    private void executeHighestEligibleProxyBid(Item item, BigDecimal newBidAmount, List<ProxyBid> eligibleProxyBids) {
        // Find the highest proxy bid that can still bid
        ProxyBid highestProxyBid = eligibleProxyBids.stream()
                .max(Comparator.comparing(ProxyBid::getMaxAmount))
                .orElse(null);

        if (highestProxyBid != null) {
            // Calculate next bid amount
            BigDecimal nextBidAmount = newBidAmount.add(item.getMinIncreasePrice());

            // Check if the highest proxy bid can afford the next bid amount
            if (nextBidAmount.compareTo(highestProxyBid.getMaxAmount()) <= 0) {
                executeProxyBid(highestProxyBid, nextBidAmount, item);

                // Check if there are competing proxy bids
                processCompetingProxyBids(item, nextBidAmount, highestProxyBid);
            } else {
                // Even the highest proxy bid is exhausted
                highestProxyBid.setStatus(ProxyBid.ProxyBidStatus.EXHAUSTED);
                highestProxyBid.setWinning(false);
                proxyBidRepository.save(highestProxyBid);
                log.info("Highest proxy bid {} exhausted by manual bid amount {}", highestProxyBid.getId(), newBidAmount);
            }
        }
    }

    private ProxyBidContext validateAndPrepareProxyBid(String userId, Long itemId, BigDecimal maxAmount) {
        // Fetch and validate entities
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item not found: " + itemId));

        // Enhanced validation: Only allow proxy bids on APPROVED and ACTIVE items
        if (item.getStatus() != Item.ItemStatus.APPROVED && item.getStatus() != Item.ItemStatus.ACTIVE) {
            throw new IllegalArgumentException("Proxy bids can only be created for items with APPROVED or ACTIVE status. Current status: " + item.getStatus());
        }

        // Validate that sellers cannot create proxy bids on their own items
        if (item.getSeller().getId().equals(userId)) {
            throw new IllegalArgumentException("Sellers cannot create proxy bids on their own items");
        }

        // Validate auction timing
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(item.getAuctionEndDate())) {
            throw new IllegalArgumentException("Cannot create proxy bid after auction has ended");
        }

        if (now.isBefore(item.getAuctionStartDate())) {
            throw new IllegalArgumentException("Cannot create proxy bid before auction starts");
        }

        // Enhanced validation: Max amount must be greater than current bid price + minimum increment
        BigDecimal currentBidPrice = item.getCurrentBidPrice() != null ?
                item.getCurrentBidPrice() : item.getStartingPrice();
        BigDecimal minimumRequired = currentBidPrice.add(item.getMinIncreasePrice());

        if (maxAmount.compareTo(minimumRequired) <= 0) {
            throw new IllegalArgumentException("Proxy bid max amount (" + maxAmount +
                                               ") must be greater than current bid price (" + currentBidPrice +
                                               ") plus minimum increment (" + item.getMinIncreasePrice() +
                                               "). Minimum required: " + minimumRequired);
        }

        return new ProxyBidContext(user, item, maxAmount, minimumRequired);
    }

    private ProxyBid createOrUpdateProxyBidRecord(ProxyBidContext context) {
        // Check for existing proxy bid
        ProxyBid existingProxyBid = proxyBidRepository.findActiveByUserAndItem(context.user().getId(), context.item().getId());

        ProxyBid proxyBid;

        if (existingProxyBid != null) {
            // Update existing proxy bid
            existingProxyBid.setMaxAmount(context.maxAmount());
            existingProxyBid.setStatus(ProxyBid.ProxyBidStatus.ACTIVE);
            existingProxyBid.setWinning(false); // Reset winning status
            proxyBid = proxyBidRepository.save(existingProxyBid);

            log.info("Updated proxy bid for user {} on item {}: max amount = {}",
                    context.user().getId(), context.item().getId(), context.maxAmount());
        } else {
            // Create new proxy bid
            ProxyBid newProxyBid = ProxyBid.builder()
                    .user(context.user())
                    .item(context.item())
                    .incrementAmount(context.item().getMinIncreasePrice())
                    .maxAmount(context.maxAmount())
                    .currentAmount(BigDecimal.ZERO)
                    .status(ProxyBid.ProxyBidStatus.ACTIVE)
                    .winning(false)
                    .build();

            proxyBid = proxyBidRepository.save(newProxyBid);

            log.info("Created new proxy bid for user {} on item {}: max amount = {}",
                    context.user().getId(), context.item().getId(), context.maxAmount());
        }

        return proxyBid;
    }

    private ExecutionStrategy determineExecutionStrategy(Item item, String userId) {
        // Find current highest bidder to determine execution strategy
        Optional<Bid> currentHighestBidOpt = bidRepository.findTopByItemOrderByAmountDesc(item);

        boolean shouldExecuteImmediately = false;

        if (currentHighestBidOpt.isPresent()) {
            Bid currentHighestBid = currentHighestBidOpt.get();
            String currentHighestBidderId = currentHighestBid.getBuyer().getId();

            // Case 1: Same person setting proxy bid - do not execute immediately
            if (currentHighestBidderId.equals(userId)) {
                log.info("Proxy bid from same user as current highest bidder {} - not executing immediately", userId);
                shouldExecuteImmediately = false;
            }
            // Case 2: Different person setting proxy bid - execute immediately
            else {
                log.info("Proxy bid from different user {} (current highest: {}) - executing immediately",
                        userId, currentHighestBidderId);
                shouldExecuteImmediately = true;
            }
        } else {
            // No current bids exist - execute proxy bid immediately to establish first bid
            log.info("No existing bids for item {} - executing proxy bid immediately", item.getId());
            shouldExecuteImmediately = true;
        }

        return new ExecutionStrategy(shouldExecuteImmediately);
    }

    private void executeProxyBidIfAffordable(ProxyBid proxyBid, Item item, BigDecimal minimumRequired) {
        BigDecimal nextBidAmount = item.getCurrentBidPrice().add(item.getMinIncreasePrice());

        // Ensure the proxy bid can afford the next bid amount
        if (nextBidAmount.compareTo(proxyBid.getMaxAmount()) <= 0) {
            executeProxyBid(proxyBid, nextBidAmount, item);
            log.info("Proxy bid executed immediately: User {} bid {} on item {}", proxyBid.getUser().getId(), nextBidAmount, item.getId());

            // Process any competing proxy bids after execution
            processCompetingProxyBids(item, nextBidAmount, proxyBid);
        } else {
            // Proxy bid cannot afford even the minimum next bid
            log.info("Proxy bid max amount {} insufficient for minimum bid {} on item {}", proxyBid.getMaxAmount(), nextBidAmount, item.getId());
            proxyBid.setStatus(ProxyBid.ProxyBidStatus.EXHAUSTED);
            proxyBidRepository.save(proxyBid);
        }
    }

    public List<ProxyBid> getEligibleProxyBids(Long itemId, BigDecimal currentAmount) {
        return proxyBidRepository.findEligibleProxyBids(itemId, currentAmount);
    }

    private void processCompetingProxyBids(Item item, BigDecimal currentAmount, ProxyBid winningProxy) {
        List<ProxyBid> competitors = proxyBidRepository.findEligibleProxyBids(item.getId(), currentAmount);

        // Remove the winning proxy bid from competitors
        competitors.removeIf(pb -> pb.getId().equals(winningProxy.getId()));

        if (competitors.isEmpty()) {
            return;
        }

        // Find the second highest proxy bid
        ProxyBid secondHighest = competitors.stream()
                .max(Comparator.comparing(ProxyBid::getMaxAmount))
                .orElse(null);

        if (secondHighest != null) {
            // Calculate competitive amount: one increment above second highest
            BigDecimal competitiveAmount = secondHighest.getMaxAmount().add(item.getMinIncreasePrice());

            // Final amount is the minimum of competitive amount and winner's max
            BigDecimal finalAmount = competitiveAmount.min(winningProxy.getMaxAmount());

            // Only bid higher if it's more than current amount
            if (finalAmount.compareTo(currentAmount) > 0) {
                executeProxyBid(winningProxy, finalAmount, item);
                log.info("Competitive proxy bidding executed: User {} outbid competitors with {}",
                        winningProxy.getUser().getId(), finalAmount);
            }

            // Mark all competing proxy bids as outbid with real-time updates
            for (ProxyBid competitor : competitors) {
                competitor.setStatus(ProxyBid.ProxyBidStatus.OUTBID);
                competitor.setWinning(false);
                proxyBidRepository.save(competitor);

                // Send notification to outbid users for immediate feedback
                try {
                    log.info("Proxy bid outbid by competition: User {} on item {} - max amount was {}",
                            competitor.getUser().getId(), item.getId(), competitor.getMaxAmount());
                } catch (Exception e) {
                    log.error("Failed to log competitive proxy bid outcome for user {}: {}",
                            competitor.getUser().getId(), e.getMessage());
                }
            }

            log.info("Processed {} competing proxy bids for item {} - all outbid by winning proxy",
                    competitors.size(), item.getId());
        }
    }

    private void executeProxyBid(ProxyBid proxyBid, BigDecimal bidAmount, Item item) {
        try {
            // Reset previous highest bid flags
            bidRepository.resetHighestBidFlags(item.getId());
            bidRepository.markPreviousBidsAsOutbid(item.getId());

            // Create the actual bid
            Bid bid = Bid.builder()
                    .item(item)
                    .buyer(proxyBid.getUser())
                    .amount(bidAmount)
                    .bidTime(LocalDateTime.now())
                    .status(Bid.BidStatus.ACCEPTED)
                    .highestBid(true)
                    .proxyBid(true)
                    .build();

            bidRepository.save(bid);

            // Update proxy bid status
            proxyBid.setCurrentAmount(bidAmount);
            proxyBid.setLastBidDate(LocalDateTime.now());
            proxyBid.setWinning(true);

            // If the bid amount equals or exceeds the max amount, mark as exhausted
            if (bidAmount.compareTo(proxyBid.getMaxAmount()) >= 0) {
                proxyBid.setStatus(ProxyBid.ProxyBidStatus.EXHAUSTED);
            }

            proxyBidRepository.save(proxyBid);

            // Update item current price
            item.setCurrentBidPrice(bidAmount);
            itemRepository.save(item);

            // === REAL-TIME SYNCHRONIZATION ===

            // 1. Update Redis cache for immediate consistency
            try {
                redisService.updateItemCache(item);
                log.debug("Updated Redis cache for item {} with new proxy bid amount {}",
                        item.getId(), bidAmount);
            } catch (Exception e) {
                log.error("Failed to update Redis cache for proxy bid on item {}: {}",
                        item.getId(), e.getMessage());
            }

            // 2. Send WebSocket notification for real-time client updates
            try {
                Long totalBids = bidRepository.countByItemId(item.getId());
                webSocketService.sendBidUpdate(bid, item, totalBids);

                log.info("Sent real-time WebSocket notification for proxy bid execution: " +
                         "Item {} - User {} - Amount {} - Total Bids: {}",
                        item.getId(), proxyBid.getUser().getId(), bidAmount, totalBids);
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for proxy bid on item {}: {}",
                        item.getId(), e.getMessage());
            }

            log.info("Proxy bid executed with real-time sync: User {} bid {} on item {}",
                    proxyBid.getUser().getId(), bidAmount, item.getId());

        } catch (Exception e) {
            log.error("Failed to execute proxy bid for user {} on item {}: {}",
                    proxyBid.getUser().getId(), item.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to execute proxy bid", e);
        }
    }

    private record ProxyBidCategories(List<ProxyBid> eligibleProxyBids, List<ProxyBid> exceededProxyBids) {
    }

    private record ProxyBidContext(User user, Item item, BigDecimal maxAmount, BigDecimal minimumRequired) {
    }

    private record ExecutionStrategy(boolean shouldExecute) {
    }
}
