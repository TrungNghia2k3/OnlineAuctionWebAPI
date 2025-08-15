package com.ntn.auction.dto.request;

import jakarta.persistence.Lob;
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
    @Size(max = 255, message = "Item name must not exceed 255 characters")
    String name;

    @NotBlank(message = "Item description is required")
    @Size(min = 10, message = "Item description must be at least 10 characters")
    @Lob
    String description;

    @NotNull(message = "Starting price is required")
    @DecimalMin(value = "0.01", message = "Starting price must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Starting price must have at most 15 integer digits and 4 decimal places")
    BigDecimal startingPrice;

    @NotNull(message = "Minimum increase price is required")
    @DecimalMin(value = "0.01", message = "Minimum increase price must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Minimum increase price must have at most 15 integer digits and 4 decimal places")
    BigDecimal reservePrice;

    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be positive")
    Long categoryId;

    @NotBlank(message = "Seller ID is required")
    String sellerId;

    @NotNull(message = "Auction start date is required")
    @Future(message = "Auction start date must be in the future")
    LocalDateTime auctionStartDate;

    @NotNull(message = "Auction end date is required")
    @Future(message = "Auction end date must be in the future")
    LocalDateTime auctionEndDate;
}
