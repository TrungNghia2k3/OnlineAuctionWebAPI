package com.ntn.auction.controller;

import com.ntn.auction.dto.response.ApiResponse;
import com.ntn.auction.dto.response.CategoryResponse;
import com.ntn.auction.dto.response.ItemResponse;
import com.ntn.auction.service.CategoryService;
import com.ntn.auction.service.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Item", description = "Item management APIs")
public class ItemController {
    ItemService itemService;

    @GetMapping
    @Operation(summary = "Get all items")
    ApiResponse<List<ItemResponse>> getAll() {
        return ApiResponse.<List<ItemResponse>>builder()
                .result(itemService.getAll())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item by ID")
    ApiResponse<ItemResponse> getById(@PathVariable Long id) {
        return ApiResponse.<ItemResponse>builder()
                .result(itemService.getById(id))
                .build();
    }
}
