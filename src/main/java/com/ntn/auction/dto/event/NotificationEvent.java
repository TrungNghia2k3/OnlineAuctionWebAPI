package com.ntn.auction.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private Long id;
    private String message;
    private Long itemId;
    private String itemName;
    private LocalDateTime notificationDate;
    private Boolean read;
}
