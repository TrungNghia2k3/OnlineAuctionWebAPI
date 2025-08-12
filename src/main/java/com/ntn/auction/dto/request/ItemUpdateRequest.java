package com.ntn.auction.dto.request;

import com.ntn.auction.entity.Item;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemUpdateRequest {
    Item.ItemStatus status;
    BigDecimal currentBidPrice;
}
