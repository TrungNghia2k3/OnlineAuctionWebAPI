package com.ntn.auction.dto.request;

import com.ntn.auction.entity.Bid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidUpdateRequest {
    private Bid.BidStatus status;
    private Boolean highestBid;
}
