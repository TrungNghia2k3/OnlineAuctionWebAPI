package com.ntn.auction.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    Long id;
    String message;
    Long itemId;
    String itemName;
    LocalDateTime notificationDate;
    Boolean read;
}
