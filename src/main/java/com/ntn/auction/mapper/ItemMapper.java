package com.ntn.auction.mapper;


import com.ntn.auction.dto.request.ItemCreateRequest;
import com.ntn.auction.dto.request.ItemAuctionUpdateRequest;
import com.ntn.auction.dto.response.ItemResponse;
import com.ntn.auction.entity.Item;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class})
public interface ItemMapper {
    @Mapping(target = "category", source = "category")
    @Mapping(target = "seller", source = "seller")
    ItemResponse mapToItemResponse(Item item);

    List<ItemResponse> mapToItemResponseList(List<Item> items);

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
    @Mapping(target = "images", ignore = true)
    void mapItemAuctionUpdate(ItemAuctionUpdateRequest request, @MappingTarget Item item);
}
