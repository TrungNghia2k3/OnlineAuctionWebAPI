package com.ntn.auction.dto.response;

import com.ntn.auction.entity.Category;
import com.ntn.auction.entity.ItemImage;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    BigDecimal startingPrice;
    BigDecimal reservePrice;
    BigDecimal minIncreasePrice;
    LocalDateTime auctionStartDate;
    LocalDateTime auctionEndDate;
    String status;
    CategoryResponse category;
    UserResponse seller;
    List<ItemImage> images;
}
