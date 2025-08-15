package com.ntn.auction.controller;

import com.ntn.auction.dto.request.ProxyBidCreateRequest;
import com.ntn.auction.dto.response.ApiResponse;
import com.ntn.auction.dto.response.ProxyBidResponse;
import com.ntn.auction.entity.ProxyBid;
import com.ntn.auction.mapper.ProxyBidMapper;
import com.ntn.auction.service.ProxyBidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/proxy-bids")
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Proxy Bid", description = "APIs for managing proxy bids")
public class ProxyBidController {

    ProxyBidService proxyBidService;
    ProxyBidMapper proxyBidMapper;

    @PostMapping
    @Operation(summary = "Create or update proxy bid", description = "Creates a new proxy bid or updates existing one for the user")
    public ApiResponse<ProxyBidResponse> createProxyBid(@Valid @RequestBody ProxyBidCreateRequest request) {

        log.info("Creating proxy bid for user {} on item {} with max amount {}",
                request.getUserId(), request.getItemId(), request.getMaxAmount());

        ProxyBid proxyBid = proxyBidService.createOrUpdateProxyBid(
                request.getUserId(),
                request.getItemId(),
                request.getMaxAmount());

        ProxyBidResponse response = proxyBidMapper.toResponse(proxyBid);

        return ApiResponse.<ProxyBidResponse>builder()
                .message("Proxy bid created successfully")
                .result(response)
                .build();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user's proxy bids", description = "Retrieves all proxy bids for a specific user")
    public ApiResponse<List<ProxyBidResponse>> getUserProxyBids(
            @PathVariable String userId) {

        List<ProxyBid> proxyBids = proxyBidService.getUserProxyBids(userId);
        List<ProxyBidResponse> responses = proxyBidMapper.toResponseList(proxyBids);

        return ApiResponse.<List<ProxyBidResponse>>builder()
                .message("User proxy bids retrieved successfully")
                .result(responses)
                .build();
    }

    @GetMapping("/item/{itemId}")
    @Operation(summary = "Get proxy bids for item", description = "Retrieves all active proxy bids for a specific item")
    public ApiResponse<List<ProxyBidResponse>> getItemProxyBids(
            @PathVariable Long itemId) {

        List<ProxyBid> proxyBids = proxyBidService.getActiveProxyBidsForItem(itemId);
        List<ProxyBidResponse> responses = proxyBidMapper.toResponseList(proxyBids);

        return ApiResponse.<List<ProxyBidResponse>>builder()
                .message("Item proxy bids retrieved successfully")
                .result(responses)
                .build();
    }

    @DeleteMapping
    @Operation(summary = "Cancel proxy bid", description = "Cancels an active proxy bid")
    public ApiResponse<Void> cancelProxyBid(
            @RequestParam Long proxyBidId,
            @RequestParam String userId
    ) {

        proxyBidService.cancelProxyBid(proxyBidId, userId);

        return ApiResponse.<Void>builder()
                .message("Proxy bid cancelled successfully")
                .build();
    }

    @GetMapping("/winning/{userId}")
    @Operation(summary = "Get winning proxy bids", description = "Retrieves all winning proxy bids for a user")
    public ApiResponse<List<ProxyBidResponse>> getWinningProxyBids(
            @PathVariable String userId) {

        List<ProxyBid> proxyBids = proxyBidService.getWinningProxyBidsByUser(userId);
        List<ProxyBidResponse> responses = proxyBidMapper.toResponseList(proxyBids);

        return ApiResponse.<List<ProxyBidResponse>>builder()
                .message("Winning proxy bids retrieved successfully")
                .result(responses)
                .build();
    }
}
