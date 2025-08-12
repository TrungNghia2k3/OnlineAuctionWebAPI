package com.ntn.auction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProxyBidRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Item ID is required")
    private Long itemId;

    @NotNull(message = "Max amount is required")
    @DecimalMin(value = "0.01", message = "Max amount must be greater than 0")
    private BigDecimal maxAmount;
}
