package com.ntn.auction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {
    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "Item ID is required")
    private Long itemId;
}
