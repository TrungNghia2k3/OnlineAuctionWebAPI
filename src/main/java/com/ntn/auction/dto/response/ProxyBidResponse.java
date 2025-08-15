package com.ntn.auction.dto.response;

import com.ntn.auction.entity.ProxyBid;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProxyBidResponse {
    Long id;
    Long itemId;
    String itemTitle;
    String userId;
    String userName;
    BigDecimal maxAmount;
    BigDecimal currentAmount;
    BigDecimal incrementAmount;
    LocalDateTime createdDate;
    LocalDateTime lastBidDate;
    ProxyBid.ProxyBidStatus status;
    Boolean winning;
}
