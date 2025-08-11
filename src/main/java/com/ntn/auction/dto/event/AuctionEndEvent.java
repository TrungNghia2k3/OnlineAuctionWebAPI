package com.ntn.auction.dto.event;

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
public class AuctionEndEvent {
    private Long itemId;
    private String itemName;
    private BigDecimal finalPrice;
    private String winnerName;
    private String winnerId;
    private LocalDateTime endTime;
    private Integer totalBids;
}
