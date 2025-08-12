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

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProxyBidService {

    ProxyBidRepository proxyBidRepository;
    BidRepository bidRepository;
    ItemRepository itemRepository;
    UserRepository userRepository;

    @Transactional
    public void processProxyBidsAfterManualBid(Item item, BigDecimal newBidAmount, User excludeUser) {
        // Get ALL active proxy bids for this item
        List<ProxyBid> allActiveProxyBids = proxyBidRepository.findActiveProxyBidsForItem(item.getId());

        // Remove proxy bids from the manual bidder to avoid self-bidding
        allActiveProxyBids.removeIf(pb -> pb.getUser().getId().equals(excludeUser.getId()));

        // Process ALL proxy bids - separate into eligible and exceeded
        List<ProxyBid> eligibleProxyBids = allActiveProxyBids.stream()
                .filter(pb -> pb.getMaxAmount().compareTo(newBidAmount) > 0)
                .toList();

        List<ProxyBid> exceededProxyBids = allActiveProxyBids.stream()
                .filter(pb -> pb.getMaxAmount().compareTo(newBidAmount) <= 0)
                .toList();

        // FIXED: Always update exceeded proxy bids to OUTBID status
        // This ensures proxy bids are properly updated even when eligibleProxyBids is empty
        for (ProxyBid exceededBid : exceededProxyBids) {
            exceededBid.setStatus(ProxyBid.ProxyBidStatus.OUTBID);
            exceededBid.setWinning(false);
            proxyBidRepository.save(exceededBid);
            log.info("Proxy bid {} exceeded by manual bid amount {} - status updated to OUTBID",
                    exceededBid.getId(), newBidAmount);
        }

        // Continue with eligible proxy bids only if any exist
        if (eligibleProxyBids.isEmpty()) {
            log.info("No eligible proxy bids remaining for item {} after manual bid {}", item.getId(), newBidAmount);
            return;
        }

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

    @Transactional
    public ProxyBid createOrUpdateProxyBid(String userId, Long itemId, BigDecimal maxAmount) {
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

        // Check for existing proxy bid
        ProxyBid existingProxyBid = proxyBidRepository.findActiveByUserAndItem(userId, itemId);

        if (existingProxyBid != null) {
            // Update existing proxy bid
            existingProxyBid.setMaxAmount(maxAmount);
            existingProxyBid.setStatus(ProxyBid.ProxyBidStatus.ACTIVE);

            log.info("Updated proxy bid for user {} on item {}: max amount = {}",
                    userId, itemId, maxAmount);
            return proxyBidRepository.save(existingProxyBid);
        } else {
            // Create new proxy bid
            ProxyBid newProxyBid = ProxyBid.builder()
                    .user(user)
                    .item(item)
                    .maxAmount(maxAmount)
                    .currentAmount(BigDecimal.ZERO)
                    .status(ProxyBid.ProxyBidStatus.ACTIVE)
                    .winning(false)
                    .build();

            log.info("Created new proxy bid for user {} on item {}: max amount = {}",
                    userId, itemId, maxAmount);
            return proxyBidRepository.save(newProxyBid);
        }
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
            }

            // Mark all competing proxy bids as outbid
            for (ProxyBid competitor : competitors) {
                competitor.setStatus(ProxyBid.ProxyBidStatus.OUTBID);
                competitor.setWinning(false);
                proxyBidRepository.save(competitor);
            }
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

            log.info("Proxy bid executed: User {} bid {} on item {}",
                    proxyBid.getUser().getId(), bidAmount, item.getId());

        } catch (Exception e) {
            log.error("Failed to execute proxy bid for user {} on item {}: {}",
                    proxyBid.getUser().getId(), item.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to execute proxy bid", e);
        }
    }
}
