package com.ntn.auction.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidNotificationPayload {
    Long bidId;
    Long itemId;
    String buyerId;
    BigDecimal amount;
    LocalDateTime timestamp;
}
