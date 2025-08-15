package com.ntn.auction.dto.response;

import com.ntn.auction.entity.Bid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidResponse {
    Long id;
    BigDecimal amount;
    LocalDateTime bidTime;
    Bid.BidStatus status;
    String buyerId;
    Long itemId;
    Boolean highestBid;
    Boolean proxyBid;
}
