package com.ntn.auction.mapper;

import com.ntn.auction.dto.event.BidUpdateEvent;
import com.ntn.auction.dto.request.BidUpdateRequest;
import com.ntn.auction.dto.request.CreateBidRequest;
import com.ntn.auction.dto.response.BidResponse;
import com.ntn.auction.entity.Bid;
import com.ntn.auction.entity.Item;
import com.ntn.auction.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BidMapper {

    @Mapping(target = "buyerName", source = "buyer.username")
    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "itemId", source = "item.id")
    @Mapping(target = "itemName", source = "item.name")
    BidResponse toResponse(Bid bid);

    @Mapping(target = "buyerName", source = "bid.buyer.username")
    @Mapping(target = "buyerId", source = "bid.buyer.id")
    @Mapping(target = "itemId", source = "bid.item.id")
    @Mapping(target = "amount", source = "bid.amount")
    @Mapping(target = "bidTime", source = "bid.bidTime")
    @Mapping(target = "totalBids", source = "totalBids")
    BidUpdateEvent toBidUpdateEvent(Bid bid, Integer totalBids);

    List<BidResponse> toResponseList(List<Bid> bids);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bidTime", ignore = true)
    @Mapping(target = "highestBid", ignore = true)
    @Mapping(target = "proxyBid", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "item", source = "item")
    @Mapping(target = "buyer", source = "buyer")
    Bid toEntity(CreateBidRequest request, Item item, User buyer);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "buyer", ignore = true)
    @Mapping(target = "amount", ignore = true)
    @Mapping(target = "bidTime", ignore = true)
    @Mapping(target = "proxyBid", ignore = true)
    void updateFromRequest(BidUpdateRequest request, @MappingTarget Bid bid);
}
