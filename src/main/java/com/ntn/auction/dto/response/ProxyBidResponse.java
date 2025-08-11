package com.ntn.auction.dto.response;

import com.ntn.auction.entity.ProxyBid;
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
public class ProxyBidResponse {

    private Long id;
    private Long itemId;
    private String itemTitle;
    private String userId;
    private String userName;
    private BigDecimal maxAmount;
    private BigDecimal currentAmount;
    private BigDecimal incrementAmount;
    private LocalDateTime createdDate;
    private LocalDateTime lastBidDate;
    private ProxyBid.ProxyBidStatus status;
    private Boolean winning;
}
