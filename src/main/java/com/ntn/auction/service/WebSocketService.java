package com.ntn.auction.service;

import com.ntn.auction.dto.BidNotificationPayload;
import com.ntn.auction.dto.event.AuctionEndEvent;
import com.ntn.auction.dto.event.BidUpdateEvent;
import com.ntn.auction.dto.response.NotificationResponse;
import com.ntn.auction.entity.Bid;
import com.ntn.auction.entity.Item;
import com.ntn.auction.entity.Notification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketService {

    SimpMessagingTemplate messagingTemplate;

    public void sendBidUpdate(Bid bid, Item item, Long totalBids) {
        try {
            BidUpdateEvent bidUpdateEvent = BidUpdateEvent.builder()
                    .bidId(bid.getId())
                    .itemId(item.getId())
                    .amount(bid.getAmount())
                    .buyerName(bid.getBuyer().getFirstName() + " " + bid.getBuyer().getLastName())
                    .buyerId(bid.getBuyer().getId())
                    .bidTime(bid.getBidTime())
                    .status(bid.getStatus())
                    .totalBids(totalBids)
                    .build();

            // Send to item-specific topic
            messagingTemplate.convertAndSend("/topic/item/" + item.getId() + "/bids", bidUpdateEvent);

            // Send to general auction updates
            messagingTemplate.convertAndSend("/topic/auctions/updates", bidUpdateEvent);

            log.debug("Sent bid update via WebSocket for item {}", item.getId());

        } catch (Exception e) {
            log.error("Failed to send bid update via WebSocket for item {}: {}", item.getId(), e.getMessage());
        }
    }

    public void sendUserNotification(String userId, Notification notification) {
        try {
            // Create notification DTO for WebSocket
            var notificationDto = NotificationResponse.builder()
                    .id(notification.getId())
                    .message(notification.getMessage())
                    .user(null) // User details can be populated separately if needed
                    .item(null) // Item details can be populated separately if needed
                    .isRead(notification.getRead()) // Use getRead() method from Notification entity
                    .notificationDate(notification.getNotificationDate())
                    .build();

            // Send to user-specific topic
            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/notifications",
                    notificationDto
            );

            log.debug("Sent user notification via WebSocket to user {}", userId);

        } catch (Exception e) {
            log.error("Failed to send user notification via WebSocket to user {}: {}", userId, e.getMessage());
        }
    }

    public void sendAuctionEndNotification(Item item, Bid winningBid) {
        try {
            AuctionEndEvent auctionEndEvent = AuctionEndEvent.builder()
                    .itemId(item.getId())
                    .itemName(item.getName())
                    .finalPrice(winningBid != null ? winningBid.getAmount() : item.getStartingPrice()) // Use finalPrice instead of winningBidAmount
                    .winnerName(winningBid != null ?
                            winningBid.getBuyer().getFirstName() + " " + winningBid.getBuyer().getLastName() :
                            "No Winner")
                    .winnerId(winningBid != null ? winningBid.getBuyer().getId() : null)
                    .endTime(LocalDateTime.now())
                    .totalBids(0L) // Set to 0 since we don't have access to total bids here
                    .build();

            // Send to item-specific topic
            messagingTemplate.convertAndSend("/topic/item/" + item.getId() + "/end", auctionEndEvent);

            // Send to general auction updates
            messagingTemplate.convertAndSend("/topic/auctions/ended", auctionEndEvent);

            // Send to seller
            messagingTemplate.convertAndSendToUser(
                    item.getSeller().getId(),
                    "/queue/auction-results",
                    auctionEndEvent
            );

            // Send to winner if exists
            if (winningBid != null) {
                messagingTemplate.convertAndSendToUser(
                        winningBid.getBuyer().getId(),
                        "/queue/auction-results",
                        auctionEndEvent
                );
            }

            log.info("Sent auction end notification for item {}", item.getId());

        } catch (Exception e) {
            log.error("Failed to send auction end notification for item {}: {}", item.getId(), e.getMessage());
        }
    }

    public void sendAuctionStatusUpdate(Item item, String status, String message) {
        try {
            var statusUpdate = java.util.Map.of(
                    "itemId", item.getId(),
                    "status", status,
                    "message", message,
                    "timestamp", LocalDateTime.now()
            );

            // Send to item-specific topic
            messagingTemplate.convertAndSend("/topic/item/" + item.getId() + "/status", statusUpdate);

            // Send to general auction updates
            messagingTemplate.convertAndSend("/topic/auctions/status", statusUpdate);

            log.debug("Sent auction status update for item {}: {}", item.getId(), status);

        } catch (Exception e) {
            log.error("Failed to send auction status update for item {}: {}", item.getId(), e.getMessage());
        }
    }

    public void sendProxyBidNotification(String userId, String message, Long itemId) {
        try {
            var proxyBidNotification = java.util.Map.of(
                    "message", message,
                    "itemId", itemId,
                    "timestamp", LocalDateTime.now(),
                    "type", "PROXY_BID"
            );

            messagingTemplate.convertAndSendToUser(
                    userId,
                    "/queue/proxy-bids",
                    proxyBidNotification
            );

            log.debug("Sent proxy bid notification to user {}", userId);

        } catch (Exception e) {
            log.error("Failed to send proxy bid notification to user {}: {}", userId, e.getMessage());
        }
    }

    public void sendSystemAnnouncement(String message) {
        try {
            var announcement = java.util.Map.of(
                    "message", message,
                    "timestamp", LocalDateTime.now(),
                    "type", "SYSTEM_ANNOUNCEMENT"
            );

            messagingTemplate.convertAndSend("/topic/system/announcements", announcement);

            log.info("Sent system announcement: {}", message);

        } catch (Exception e) {
            log.error("Failed to send system announcement: {}", e.getMessage());
        }
    }

    @Async("webSocketExecutor")
    public void sendBidUpdateAsync(BidNotificationPayload payload) {
        try {
            BidUpdateEvent bidUpdateEvent = BidUpdateEvent.builder()
                    .bidId(payload.getBidId())
                    .itemId(payload.getItemId())
                    .amount(payload.getAmount())
                    .buyerId(payload.getBuyerId())
                    .bidTime(payload.getTimestamp())
                    .status(Bid.BidStatus.ACCEPTED) // Temporary status during async processing
                    .build();

            // Send to item-specific topic
            messagingTemplate.convertAndSend("/topic/item/" + payload.getItemId() + "/bids", bidUpdateEvent);

            // Send to general auction updates
            messagingTemplate.convertAndSend("/topic/auctions/updates", bidUpdateEvent);

            log.debug("Sent immediate async bid update for bid {}", payload.getBidId());

        } catch (Exception e) {
            log.error("Failed to send immediate bid update for bid {}: {}", payload.getBidId(), e.getMessage());
        }
    }

    @Async("webSocketExecutor")
    public void sendBidCompletionNotification(Bid bid, Item item, Long totalBids) {
        try {
            BidUpdateEvent finalUpdateEvent = BidUpdateEvent.builder()
                    .bidId(bid.getId())
                    .itemId(item.getId())
                    .amount(bid.getAmount())
                    .buyerName(bid.getBuyer().getFirstName() + " " + bid.getBuyer().getLastName())
                    .buyerId(bid.getBuyer().getId())
                    .bidTime(bid.getBidTime())
                    .status(bid.getStatus())
                    .totalBids(totalBids)
                    .build();

            // Send to item-specific topic
            messagingTemplate.convertAndSend("/topic/item/" + item.getId() + "/bids", finalUpdateEvent);

            // Send to general auction updates
            messagingTemplate.convertAndSend("/topic/auctions/updates", finalUpdateEvent);

            log.debug("Sent final bid completion notification for bid {}", bid.getId());

        } catch (Exception e) {
            log.error("Failed to send bid completion notification for bid {}: {}", bid.getId(), e.getMessage());
        }
    }

    @Async("webSocketExecutor")
    public void sendBidErrorNotification(String bidId, Long itemId, String buyerId, String errorMessage) {
        try {
            var errorNotification = java.util.Map.of(
                    "bidId", bidId,
                    "itemId", itemId,
                    "buyerId", buyerId,
                    "error", errorMessage,
                    "status", "FAILED",
                    "timestamp", LocalDateTime.now()
            );

            // Send error to specific user
            messagingTemplate.convertAndSendToUser(
                    buyerId,
                    "/queue/bid-errors",
                    errorNotification
            );

            // Send to item-specific topic for admin monitoring
            messagingTemplate.convertAndSend("/topic/item/" + itemId + "/errors", errorNotification);

            log.warn("Sent bid error notification for failed bid {}", bidId);

        } catch (Exception e) {
            log.error("Failed to send bid error notification for bid {}: {}", bidId, e.getMessage());
        }
    }
}
