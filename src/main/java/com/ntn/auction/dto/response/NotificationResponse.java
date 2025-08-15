package com.ntn.auction.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NotificationResponse {
    Long id;
    String message;
    boolean isRead;
    UserResponse user;
    ItemResponse item;
    LocalDateTime notificationDate;
}
