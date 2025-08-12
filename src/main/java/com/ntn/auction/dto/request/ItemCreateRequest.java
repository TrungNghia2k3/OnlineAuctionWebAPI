package com.ntn.auction.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemCreateRequest {

    @NotBlank(message = "Item name is required")
    @Size(min = 3, max = 100, message = "Item name must be between 3 and 100 characters")
    String name;

    @NotBlank(message = "Item description is required")
    @Size(min = 10, max = 5000, message = "Item description must be between 10 and 5000 characters")
    String description;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Starting price must have at most 15 integer digits and 4 decimal places")
    BigDecimal startingPrice;

    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be positive")
    Long categoryId;

    @NotBlank(message = "Seller ID is required")
    String sellerId;

    @NotNull(message = "Auction start date is required")
    @Future(message = "Auction start date must be in the future")
    LocalDateTime auctionStartDate;

    @NotNull(message = "Auction end date is required")
    LocalDateTime auctionEndDate;

    // Image URL will be set separately after image upload
    String imageUrl;
}
