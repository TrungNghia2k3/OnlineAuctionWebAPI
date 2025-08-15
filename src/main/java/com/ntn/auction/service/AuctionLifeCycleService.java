package com.ntn.auction.service;

import com.ntn.auction.dto.request.ItemAuctionUpdateRequest;
import com.ntn.auction.entity.Bid;
import com.ntn.auction.entity.Item;
import com.ntn.auction.mapper.ItemMapper;
import com.ntn.auction.repository.BidRepository;
import com.ntn.auction.repository.ItemRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuctionLifeCycleService {
    // Repositories
    ItemRepository itemRepository;
    BidRepository bidRepository;

    // Mappers
    ItemMapper itemMapper;

    // Focused Service following SRP
    NotificationService notificationService;
    BidService bidService;

    // Scheduled task to process the auction lifecycle every 5 minutes
    // - Move items from PENDING to UPCOMING/ACTIVE
    // - Move items from UPCOMING to ACTIVE
    // - End ACTIVE auctions to SOLD/EXPIRED

    @Scheduled(fixedRate = 300000) // 5 minutes = 300,000ms
    public void processAuctionLifecycle() {
        log.info("Start processing the auction lifecycle");

        try {
            // 1. Process items from PENDING -> UPCOMING/ACTIVE
            processApprovedItems();

            // 2. Process items from UPCOMING -> ACTIVE
            processUpcomingItems();

            // 3. Auction closing processing ACTIVE -> SOLD/EXPIRED
            processActiveItems();

            log.info("Complete auction lifecycle processing");

        } catch (Exception e) {
            log.error("Error while processing auction lifecycle: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process auction lifecycle", e);
        }
    }

    private void processApprovedItems() {
        LocalDateTime now = LocalDateTime.now();

        // Find all items that are approved and have auction start date before now
        List<Item> approvedItems = itemRepository.findByStatusAndAuctionStartDateBefore(Item.ItemStatus.PENDING, now);

        for (Item item : approvedItems) {
            ItemAuctionUpdateRequest updateRequest = ItemAuctionUpdateRequest.builder().build();

            if (item.getAuctionStartDate().isBefore(now) || item.getAuctionStartDate().isEqual(now)) {
                updateRequest.setStatus(Item.ItemStatus.ACTIVE);
                log.info("Change item {} to ACTIVE state", item.getId());
            } else {
                updateRequest.setStatus(Item.ItemStatus.UPCOMING);
                log.info("Change item {} to UPCOMING status", item.getId());
            }

            itemMapper.mapItemAuctionUpdate(updateRequest, item);
        }

        if (!approvedItems.isEmpty()) {
            itemRepository.saveAll(approvedItems);
        }
    }

    private void processUpcomingItems() {
        LocalDateTime now = LocalDateTime.now();

        List<Item> upcomingItems = itemRepository.findByStatusAndAuctionStartDateBefore(
                Item.ItemStatus.UPCOMING, now);

        for (Item item : upcomingItems) {
            ItemAuctionUpdateRequest updateRequest = ItemAuctionUpdateRequest.builder()
                    .status(Item.ItemStatus.ACTIVE)
                    .build();

            itemMapper.mapItemAuctionUpdate(updateRequest, item);
            log.info("Move item {} from UPCOMING to ACTIVE", item.getId());
        }

        if (!upcomingItems.isEmpty()) {
            itemRepository.saveAll(upcomingItems);
        }
    }

    private void processActiveItems() {
        LocalDateTime now = LocalDateTime.now();

        List<Item> expiredItems = itemRepository.findByStatusAndAuctionEndDateBefore(
                Item.ItemStatus.ACTIVE, now);

        for (Item item : expiredItems) {
            processAuctionEnd(item);
        }
    }

    private void processAuctionEnd(Item item) {
        // Find the highest bid for this item
        Optional<Bid> highestBidOpt = bidRepository.findTopByItemOrderByAmountDesc(item);

        if (highestBidOpt.isPresent()) {
            Bid winningBid = highestBidOpt.get();

            // Update item status to SOLD
            ItemAuctionUpdateRequest itemUpdate = ItemAuctionUpdateRequest.builder()
                    .status(Item.ItemStatus.SOLD)
                    .currentBidPrice(winningBid.getAmount())
                    .build();
            itemMapper.mapItemAuctionUpdate(itemUpdate, item);

            // Update the winning bid status
            bidService.updateBidsStatus(item, winningBid);

            // Create notifications for the winning bid
            notificationService.createNotifications(item, winningBid.getBuyer());

            // Send WebSocket notification for auction end
            notificationService.sendAuctionEndNotification(item, winningBid);

            log.info("Item {} was sold to user {} for {}",
                    item.getId(), winningBid.getBuyer().getId(), winningBid.getAmount());

        } else {
            // No bids were placed, mark item as EXPIRED
            ItemAuctionUpdateRequest itemUpdate = ItemAuctionUpdateRequest.builder()
                    .status(Item.ItemStatus.EXPIRED)
                    .build();
            itemMapper.mapItemAuctionUpdate(itemUpdate, item);

            notificationService.createExpiredNotification(item);

            // Send WebSocket notification for auction end with no bids
            notificationService.sendAuctionEndNotification(item, null);

            log.info("Item {} has expired with no bid", item.getId());
        }

        itemRepository.save(item);
    }
}
