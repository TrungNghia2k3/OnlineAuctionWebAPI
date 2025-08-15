package com.ntn.auction.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class AuctionEndEvent {
    Long itemId;
    String itemName;
    BigDecimal finalPrice;
    String winnerName;
    String winnerId;
    LocalDateTime endTime;
    Long totalBids;
}
