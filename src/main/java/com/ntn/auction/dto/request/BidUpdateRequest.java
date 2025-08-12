package com.ntn.auction.dto.request;

import com.ntn.auction.entity.Bid;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidUpdateRequest {
    Bid.BidStatus status;
    Boolean highestBid;
}
