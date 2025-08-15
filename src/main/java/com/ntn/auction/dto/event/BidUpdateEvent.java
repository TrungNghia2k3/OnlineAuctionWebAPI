package com.ntn.auction.dto.event;

import com.ntn.auction.entity.Bid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidUpdateEvent {
    Long itemId;
    Long bidId;
    BigDecimal amount;
    LocalDateTime bidTime;
    String buyerName;
    String buyerId;
    Bid.BidStatus status;
    Long totalBids;
}
