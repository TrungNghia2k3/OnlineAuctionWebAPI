package com.ntn.auction.dto.request;

import jakarta.validation.constraints.*;
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
public class CreateItemRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private String imageUrl;

    @NotNull(message = "Minimum increase price is required")
    @DecimalMin(value = "0.01", message = "Minimum increase price must be greater than 0")
    private BigDecimal minIncreasePrice;

    @NotNull(message = "Auction start date is required")
    @Future(message = "Auction start date must be in the future")
    private LocalDateTime auctionStartDate;

    @NotNull(message = "Auction end date is required")
    @Future(message = "Auction end date must be in the future")
    private LocalDateTime auctionEndDate;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
