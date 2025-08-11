package com.ntn.auction.mapper;

import com.ntn.auction.dto.response.ProxyBidResponse;
import com.ntn.auction.entity.ProxyBid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProxyBidMapper {

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.name", target = "itemTitle")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.firstName", target = "userName")
    ProxyBidResponse toResponse(ProxyBid proxyBid);

    List<ProxyBidResponse> toResponseList(List<ProxyBid> proxyBids);
}
