package com.ntn.auction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateProxyBidRequest {

    @NotBlank(message = "User ID is required")
    String userId;

    @NotNull(message = "Item ID is required")
    Long itemId;

    @NotNull(message = "Max amount is required")
    @DecimalMin(value = "0.01", message = "Max amount must be greater than 0")
    BigDecimal maxAmount;
}
