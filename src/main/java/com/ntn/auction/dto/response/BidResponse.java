package com.ntn.auction.dto.response;

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
public class BidResponse {
    private Long id;
    private BigDecimal amount;
    private LocalDateTime bidTime;
    private Bid.BidStatus status;
    private String buyerName;
    private String buyerId;
    private Long itemId;
    private String itemName;
    private Boolean highestBid;
}
