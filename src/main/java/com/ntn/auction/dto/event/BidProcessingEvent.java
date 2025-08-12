package com.ntn.auction.dto.event;

import com.ntn.auction.dto.BidValidationResult;
import com.ntn.auction.dto.request.CreateBidRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BidProcessingEvent {
    Long bidId;
    CreateBidRequest createBidRequest;
    BidValidationResult validationResult;
    String ipAddress;
    LocalDateTime timestamp;
}
