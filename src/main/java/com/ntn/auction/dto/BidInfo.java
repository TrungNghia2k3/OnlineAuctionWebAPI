package com.ntn.auction.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidInfo {
    Long bidId;
    Long dbBidId; // Set after DB persistence
    String buyerId;
    Long itemId;
    BigDecimal amount;
    Long timestamp;
}
