package com.ntn.auction.service;

import com.ntn.auction.dto.request.ItemUpdateRequest;
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

    // // Focused Service following SRP
    NotificationService notificationService;
    BidService bidService;

    /**
     * Scheduled task to process the auction lifecycle every 5 minutes
     * - Move items from PENDING to UPCOMING/ACTIVE
     * - Move items from UPCOMING to ACTIVE
     * - End ACTIVE auctions to SOLD/EXPIRED
     */
    @Scheduled(fixedRate = 300000) // 5 phút = 300,000ms
    public void processAuctionLifecycle() {
        log.info("Start processing the auction lifecycle");

        try {
            // 1. Xử lý items từ PENDING -> UPCOMING/ACTIVE
            processApprovedItems();

            // 2. Xử lý items từ UPCOMING -> ACTIVE
            processUpcomingItems();

            // 3. Xử lý kết thúc đấu giá ACTIVE -> SOLD/EXPIRED
            processActiveItems();

            log.info("Complete auction lifecycle processing");

        } catch (Exception e) {
            log.error("Error while processing auction lifecycle: {}", e.getMessage(), e);
        }
    }

    private void processApprovedItems() {
        LocalDateTime now = LocalDateTime.now();

        // Tìm các item đã được duyệt (giả sử có trạng thái APPROVED)
        // Chú ý: Trong entity của bạn không có APPROVED, có thể cần thêm vào enum
        List<Item> approvedItems = itemRepository.findByStatusAndAuctionStartDateBefore(
                Item.ItemStatus.PENDING, now);

        for (Item item : approvedItems) {
            ItemUpdateRequest updateRequest = ItemUpdateRequest.builder().build();

            if (item.getAuctionStartDate().isBefore(now) || item.getAuctionStartDate().isEqual(now)) {
                updateRequest.setStatus(Item.ItemStatus.ACTIVE);
                log.info("Change item {} to ACTIVE state", item.getId());
            } else {
                updateRequest.setStatus(Item.ItemStatus.UPCOMING);
                log.info("Change item {} to UPCOMING status", item.getId());
            }

            itemMapper.updateFromRequest(updateRequest, item);
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
            ItemUpdateRequest updateRequest = ItemUpdateRequest.builder()
                    .status(Item.ItemStatus.ACTIVE)
                    .build();

            itemMapper.updateFromRequest(updateRequest, item);
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
        // Tìm bid cao nhất cho item này
        Optional<Bid> highestBidOpt = bidRepository.findTopByItemOrderByAmountDesc(item);

        if (highestBidOpt.isPresent()) {
            Bid winningBid = highestBidOpt.get();

            // Cập nhật item thành SOLD
            ItemUpdateRequest itemUpdate = ItemUpdateRequest.builder()
                    .status(Item.ItemStatus.SOLD)
                    .currentBidPrice(winningBid.getAmount())
                    .build();
            itemMapper.updateFromRequest(itemUpdate, item);

            // Cập nhật tất cả bids của item này
            bidService.updateBidsStatus(item, winningBid);

            // Tạo notifications
            notificationService.createNotifications(item, winningBid.getBuyer());

            // Gửi thông báo WebSocket cho kết thúc đấu giá
            notificationService.sendAuctionEndNotification(item, winningBid);

            log.info("Item {} was sold to user {} for {}",
                    item.getId(), winningBid.getBuyer().getId(), winningBid.getAmount());

        } else {
            // Không có bid nào -> EXPIRED
            ItemUpdateRequest itemUpdate = ItemUpdateRequest.builder()
                    .status(Item.ItemStatus.EXPIRED)
                    .build();
            itemMapper.updateFromRequest(itemUpdate, item);

            notificationService.createExpiredNotification(item);

            // Gửi thông báo WebSocket cho đấu giá hết hạn
            notificationService.sendAuctionEndNotification(item, null);

            log.info("Item {} has expired with no bid", item.getId());
        }

        itemRepository.save(item);
    }
}
