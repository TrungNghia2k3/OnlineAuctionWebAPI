package com.ntn.auction.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import com.ntn.auction.dto.response.ApiResponse;
import com.ntn.auction.dto.request.PermissionRequest;
import com.ntn.auction.dto.response.PermissionResponse;
import com.ntn.auction.service.PermissionService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Permission", description = "Endpoints for managing permissions")
public class PermissionController {
    PermissionService permissionService;

    @PostMapping
    @Operation(summary = "Create a new permission", security = {@SecurityRequirement(name = "bearerAuth")})
    ApiResponse<PermissionResponse> create(@RequestBody PermissionRequest request) {
        return ApiResponse.<PermissionResponse>builder().result(permissionService.create(request)).build();
    }

    @GetMapping
    @Operation(summary = "Get all permissions", security = {@SecurityRequirement(name = "bearerAuth")})
    ApiResponse<List<PermissionResponse>> getAll() {
        return ApiResponse.<List<PermissionResponse>>builder().result(permissionService.getAll()).build();
    }

    @DeleteMapping("/{permission}")
    @Operation(summary = "Delete a permission by name", security = {@SecurityRequirement(name = "bearerAuth")})
    ApiResponse<String> delete(@PathVariable String permission) {
        permissionService.delete(permission);
        return ApiResponse.<String>builder().result("Permission deleted successfully").build();
    }
}
