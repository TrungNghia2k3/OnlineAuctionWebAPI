package com.ntn.auction.service;

import com.ntn.auction.dto.event.AuctionEndEvent;
import com.ntn.auction.dto.event.BidUpdateEvent;
import com.ntn.auction.entity.Bid;
import com.ntn.auction.entity.Item;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WebSocketService {

    SimpMessagingTemplate messagingTemplate;

    /**
     * Send bid update to all subscribers of an item
     */
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

    /**
     * Send auction end notification
     */
    public void sendAuctionEnd(Item item, Bid winningBid, Long totalBids) {
        try {
            AuctionEndEvent event = AuctionEndEvent.builder()
                    .itemId(item.getId())
                    .itemName(item.getName())
                    .finalPrice(winningBid != null ? winningBid.getAmount() : null)
                    .winnerName(winningBid != null ? winningBid.getBuyer().getUsername() : null)
                    .winnerId(winningBid != null ? winningBid.getBuyer().getId() : null)
                    .endTime(LocalDateTime.now())
                    .totalBids(totalBids)
                    .build();

            messagingTemplate.convertAndSend("/topic/item/" + item.getId() + "/end", event);

            log.info("Sent auction end notification via WebSocket for item {}", item.getId());

        } catch (Exception e) {
            log.error("Failed to send auction end notification via WebSocket for item {}: {}", item.getId(), e.getMessage());
        }
    }

    /**
     * Send general notification to a specific user
     */
    public void sendUserNotification(String userId, Object notification) {
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", notification);
            log.debug("Sent notification to user {}", userId);
        } catch (Exception e) {
            log.error("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }
}
