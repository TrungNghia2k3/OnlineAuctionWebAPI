package com.ntn.auction.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemCreateRequest {
    private String name;
    private String description;
    private String imageUrl;
    private Double minIncreasePrice;
    private String auctionStartDate; // ISO 8601 format
    private String auctionEndDate; // ISO 8601 format
    private Integer antiSnipeExtensionMinutes;
    private Integer antiSnipeThresholdMinutes;
    private Integer maxExtensions;
    private Double reservePrice;
    private Double startingPrice;
    private Long categoryId;

    // Getters and Setters
}
