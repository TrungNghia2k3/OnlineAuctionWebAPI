package com.ntn.auction.controller;

import com.ntn.auction.dto.request.PermissionRequest;
import com.ntn.auction.dto.response.ApiResponse;
import com.ntn.auction.dto.response.CategoryResponse;
import com.ntn.auction.dto.response.PermissionResponse;
import com.ntn.auction.service.CategoryService;
import com.ntn.auction.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Category", description = "Category management APIs")
public class CategoryController {
    CategoryService categoryService;

    @GetMapping
    ApiResponse<List<CategoryResponse>> getAll() {
        return ApiResponse.<List<CategoryResponse>>builder().result(categoryService.getAll()).build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    ApiResponse<CategoryResponse> getById(@PathVariable Long id) {
        return ApiResponse.<CategoryResponse>builder().result(categoryService.getById(id)).build();
    }
}
