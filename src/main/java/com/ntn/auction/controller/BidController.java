package com.ntn.auction.controller;

import com.ntn.auction.dto.request.CreateBidRequest;
import com.ntn.auction.dto.response.ApiResponse;
import com.ntn.auction.dto.response.BidResponse;
import com.ntn.auction.service.BidService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Bid", description = "APIs for managing bids on items")
public class BidController {

    BidService bidService;

    @PostMapping
    public ApiResponse<BidResponse> placeBid(@RequestBody @Valid CreateBidRequest createBidRequest) {
        BidResponse bid = bidService.placeBid(createBidRequest);
        return ApiResponse.<BidResponse>builder().result(bid).build();
    }

    @GetMapping("/item/{itemId}")
    public ApiResponse<List<BidResponse>> getItemBids(@PathVariable Long itemId) {
        List<BidResponse> bids = bidService.getItemBids(itemId);
        return ApiResponse.<List<BidResponse>>builder().result(bids).build();
    }
}
