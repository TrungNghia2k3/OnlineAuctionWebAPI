package com.ntn.auction.service;

import com.ntn.auction.dto.event.AuctionEndEvent;
import com.ntn.auction.dto.request.NotificationCreateRequest;
import com.ntn.auction.dto.response.NotificationResponse;
import com.ntn.auction.entity.Bid;
import com.ntn.auction.entity.Item;
import com.ntn.auction.entity.Notification;
import com.ntn.auction.entity.User;
import com.ntn.auction.mapper.NotificationMapper;
import com.ntn.auction.repository.BidRepository;
import com.ntn.auction.repository.NotificationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class NotificationService {

    NotificationRepository notificationRepository;
    BidRepository bidRepository;
    NotificationMapper notificationMapper;
    SimpMessagingTemplate messagingTemplate;


    public List<NotificationResponse> getUserNotifications(String userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByNotificationDateDesc(userId);
        return notificationMapper.toResponseList(notifications);
    }

    public List<NotificationResponse> getUnreadNotifications(String userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByUserIdAndReadFalseOrderByNotificationDateDesc(userId);
        return notificationMapper.toResponseList(unreadNotifications);
    }

    public Integer getUnreadCount(String userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void createNotifications(Item item, User winner) {
        // Notification cho seller
        NotificationCreateRequest sellerRequest = NotificationCreateRequest.builder()
                .message(String.format("Your product '%s' was successfully sold for price %s",
                        item.getName(), item.getCurrentBidPrice()))
                .userId(item.getSeller().getId())
                .itemId(item.getId())
                .build();

        // Notification cho winner
        NotificationCreateRequest winnerRequest = NotificationCreateRequest.builder()
                .message(String.format("Congratulations! You have won the auction for product '%s' with price %s",
                        item.getName(), item.getCurrentBidPrice()))
                .userId(winner.getId())
                .itemId(item.getId())
                .build();

        Notification sellerNotification = notificationMapper.toEntity(sellerRequest);
        Notification winnerNotification = notificationMapper.toEntity(winnerRequest);

        notificationRepository.saveAll(Arrays.asList(sellerNotification, winnerNotification));
    }

    public void createExpiredNotification(Item item) {
        NotificationCreateRequest expiredRequest = NotificationCreateRequest.builder()
                .message(String.format("Your product '%s' has expired without a buyer",
                        item.getName()))
                .userId(item.getSeller().getId())
                .itemId(item.getId())
                .build();

        Notification expiredNotification = notificationMapper.toEntity(expiredRequest);
        notificationRepository.save(expiredNotification);
    }

    public void sendAuctionEndNotification(Item item, Bid winningBid) {
        AuctionEndEvent event = AuctionEndEvent.builder()
                .itemId(item.getId())
                .itemName(item.getName())
                .finalPrice(winningBid != null ? winningBid.getAmount() : null)
                .winnerName(winningBid != null ? winningBid.getBuyer().getUsername() : null)
                .winnerId(winningBid != null ? winningBid.getBuyer().getId() : null)
                .endTime(LocalDateTime.now())
                .totalBids(bidRepository.countByItemId(item.getId()))
                .build();

        messagingTemplate.convertAndSend("/topic/item/" + item.getId() + "/end", event);
        log.info("Sent auction end notification for item {}", item.getId());
    }
}
