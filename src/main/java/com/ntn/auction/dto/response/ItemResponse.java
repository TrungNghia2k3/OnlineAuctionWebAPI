package com.ntn.auction.dto.response;

import com.ntn.auction.entity.Category;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemResponse {
    Long id;
    String name;
    String description;
    String imageUrl;
    BigDecimal minIncreasePrice;
    LocalDateTime auctionStartDate;
    LocalDateTime auctionEndDate;
    String status;
    CategoryResponse category;
    UserResponse seller;
}
