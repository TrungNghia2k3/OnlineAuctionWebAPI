package com.ntn.auction.dto.event;

import com.ntn.auction.entity.Bid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidUpdateEvent {
    private Long itemId;
    private Long bidId;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private String buyerName;
    private String buyerId;
    private Bid.BidStatus status;
    private Long totalBids;
}
