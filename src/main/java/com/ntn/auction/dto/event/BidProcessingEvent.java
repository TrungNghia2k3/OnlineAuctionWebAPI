package com.ntn.auction.dto.event;

import com.ntn.auction.dto.BidValidationResult;
import com.ntn.auction.dto.request.BidCreateRequest;
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
    BidCreateRequest bidCreateRequest;
    BidValidationResult validationResult;
    String ipAddress;
    LocalDateTime timestamp;
}
