package com.ntn.auction.dto.request;

import com.ntn.auction.entity.Bid;
import com.ntn.auction.entity.Item;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemUpdateRequest {
    private Item.ItemStatus status;
    private BigDecimal currentBidPrice;
}
