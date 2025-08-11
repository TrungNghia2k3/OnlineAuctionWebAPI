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
                .max((pb1, pb2) -> pb1.getMaxAmount().compareTo(pb2.getMaxAmount()))
                .orElse(null);

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

    @Transactional
    public ProxyBid createOrUpdateProxyBid(String userId, Long itemId, BigDecimal maxAmount) {
        // Check if user already has an active proxy bid for this item
        var existingProxyBid = proxyBidRepository.findActiveProxyBidByUserAndItem(userId, itemId);

        if (existingProxyBid.isPresent()) {
            ProxyBid existing = existingProxyBid.get();
            existing.setMaxAmount(maxAmount);
            existing.setStatus(ProxyBid.ProxyBidStatus.ACTIVE);
            log.info("Updated proxy bid for user {} on item {} with max amount {}", userId, itemId, maxAmount);
            return proxyBidRepository.save(existing);
        }

        // Fetch entities
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item not found: " + itemId));

        // Create new proxy bid
        ProxyBid proxyBid = ProxyBid.builder()
                .user(user)
                .item(item)
                .maxAmount(maxAmount)
                .incrementAmount(item.getMinIncreasePrice()) // Use item's minimum increment
                .status(ProxyBid.ProxyBidStatus.ACTIVE)
                .winning(false)
                .build();

        log.info("Created new proxy bid for user {} on item {} with max amount {}", userId, itemId, maxAmount);
        return proxyBidRepository.save(proxyBid);
    }

    public List<ProxyBid> getUserProxyBids(String userId) {
        return proxyBidRepository.findByUserId(userId);
    }

    public List<ProxyBid> getActiveProxyBidsForItem(Long itemId) {
        return proxyBidRepository.findActiveProxyBidsForItem(itemId);
    }

    @Transactional
    public void cancelProxyBid(Long proxyBidId) {
        ProxyBid proxyBid = proxyBidRepository.findById(proxyBidId)
                .orElseThrow(() -> new RuntimeException("Proxy bid not found: " + proxyBidId));

        proxyBid.setStatus(ProxyBid.ProxyBidStatus.CANCELLED);
        proxyBid.setWinning(false);
        proxyBidRepository.save(proxyBid);

        log.info("Cancelled proxy bid {} for user {}", proxyBidId, proxyBid.getUser().getId());
    }

    public List<ProxyBid> getWinningProxyBidsByUser(String userId) {
        return proxyBidRepository.findWinningProxyBidsByUser(userId);
    }

    private void processCompetingProxyBids(Item item, BigDecimal currentAmount, ProxyBid winningProxy) {
        List<ProxyBid> competitors = proxyBidRepository
                .findEligibleProxyBids(item.getId(), currentAmount);

        competitors.removeIf(pb -> pb.getId().equals(winningProxy.getId()));

        if (competitors.isEmpty()) {
            return;
        }

        ProxyBid secondHighest = competitors.getFirst();
        BigDecimal competitiveAmount = secondHighest.getMaxAmount().add(item.getMinIncreasePrice());

        // Bid up to one increment above the second highest, or the winner's max
        BigDecimal finalAmount = competitiveAmount.min(winningProxy.getMaxAmount());

        if (finalAmount.compareTo(currentAmount) > 0) {
            executeProxyBid(winningProxy, finalAmount, item);
        }

        // Mark competing proxy bids as outbid
        for (ProxyBid competitor : competitors) {
            competitor.setStatus(ProxyBid.ProxyBidStatus.OUTBID);
            proxyBidRepository.save(competitor);
        }
    }

    private void executeProxyBid(ProxyBid proxyBid, BigDecimal bidAmount, Item item) {
        try {
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

            // If the bid amount matches the max amount, mark as exhausted
            if (bidAmount.equals(proxyBid.getMaxAmount())) {
                proxyBid.setStatus(ProxyBid.ProxyBidStatus.EXHAUSTED);
            }

            proxyBidRepository.save(proxyBid);

            // Update item current price
            item.setCurrentBidPrice(bidAmount);

            log.info("Proxy bid executed: {} bid {} on item {}", proxyBid.getUser().getId(), bidAmount, item.getId());

        } catch (Exception e) {
            log.error("Failed to execute proxy bid for user {} on item {}: {}", proxyBid.getUser().getId(), item.getId(), e.getMessage());
        }
    }
}
