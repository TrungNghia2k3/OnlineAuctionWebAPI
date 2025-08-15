package com.ntn.auction.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionStartEvent {
    Long itemId;
    String itemName;
    BigDecimal startingPrice;
    LocalDateTime startTime;
    LocalDateTime endTime;
}
