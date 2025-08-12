package com.ntn.auction.mapper;


import com.ntn.auction.dto.request.ItemUpdateRequest;
import com.ntn.auction.dto.response.ItemResponse;
import com.ntn.auction.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class})
public interface ItemMapper {

    /**
     * Convert Item entity to ItemResponse DTO
     */
    @Mapping(target = "category", source = "category")
    @Mapping(target = "seller", source = "seller")
    ItemResponse toResponse(Item item);

    /**
     * Convert list of Item entities to list of ItemResponse DTOs
     */
    List<ItemResponse> toResponseList(List<Item> items);

    /**
     * Update existing Item entity from ItemResponse DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "originalEndDate", ignore = true)
    @Mapping(target = "antiSnipeExtensionMinutes", ignore = true)
    @Mapping(target = "antiSnipeThresholdMinutes", ignore = true)
    @Mapping(target = "maxExtensions", ignore = true)
    @Mapping(target = "currentExtensions", ignore = true)
    @Mapping(target = "reservePrice", ignore = true)
    @Mapping(target = "startingPrice", ignore = true)
    @Mapping(target = "reserveMet", ignore = true)
    @Mapping(target = "currentBidPrice", ignore = true)
    void updateFromResponse(ItemResponse itemResponse, @MappingTarget Item item);

    /**
     * Update existing Item entity from ItemUpdateRequest DTO
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "minIncreasePrice", ignore = true)
    @Mapping(target = "auctionStartDate", ignore = true)
    @Mapping(target = "auctionEndDate", ignore = true)
    @Mapping(target = "originalEndDate", ignore = true)
    @Mapping(target = "antiSnipeExtensionMinutes", ignore = true)
    @Mapping(target = "antiSnipeThresholdMinutes", ignore = true)
    @Mapping(target = "maxExtensions", ignore = true)
    @Mapping(target = "currentExtensions", ignore = true)
    @Mapping(target = "reservePrice", ignore = true)
    @Mapping(target = "startingPrice", ignore = true)
    @Mapping(target = "reserveMet", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "seller", ignore = true)
    void updateFromRequest(ItemUpdateRequest request, @MappingTarget Item item);
}
