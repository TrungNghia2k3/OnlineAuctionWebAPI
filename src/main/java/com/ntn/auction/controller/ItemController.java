package com.ntn.auction.controller;

import com.ntn.auction.dto.request.ItemCreateRequest;
import com.ntn.auction.dto.response.ApiResponse;
import com.ntn.auction.dto.response.ItemResponse;
import com.ntn.auction.service.ImagesStorageService;
import com.ntn.auction.service.ItemService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ItemController {

    ItemService itemService;
    ImagesStorageService imagesStorageService;

    @PostMapping
    public ApiResponse<ItemResponse> createItem(@Valid @RequestBody ItemCreateRequest request, Authentication authentication) {

        try {
            String sellerId = authentication.getName();
            log.info("Creating new item for seller: {}", sellerId);

            ItemResponse createdItem = itemService.createItem(request, sellerId);

            log.info("Item created successfully with ID: {}", createdItem.getId());

            return ApiResponse.<ItemResponse>builder()
                    .code(201)
                    .message("Item created successfully")
                    .result(createdItem)
                    .build();

        } catch (IllegalArgumentException e) {
            log.error("Validation error creating item: {}", e.getMessage());
            return ApiResponse.<ItemResponse>builder()
                    .code(400)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error creating item: {}", e.getMessage(), e);
            return ApiResponse.<ItemResponse>builder()
                    .code(500)
                    .message("Failed to create item: " + e.getMessage())
                    .build();
        }
    }

    @PostMapping("/with-image")
    public ApiResponse<ItemResponse> createItemWithImage(@Valid @ModelAttribute ItemCreateRequest request, @RequestParam("image") MultipartFile imageFile, Authentication authentication) {

        try {
            String sellerId = authentication.getName();
            log.info("Creating new item with image for seller: {}", sellerId);

            // Validate image
            if (imageFile.isEmpty()) {
                return ApiResponse.<ItemResponse>builder()
                        .code(400)
                        .message("Please select an image file")
                        .build();
            }

            if (imagesStorageService.isValidImageType(imageFile)) {
                return ApiResponse.<ItemResponse>builder()
                        .code(400)
                        .message("Invalid image type. Only JPG, JPEG, PNG, GIF, and WEBP are allowed")
                        .build();
            }

            // Create item first to get ID
            ItemResponse createdItem = itemService.createItem(request, sellerId);

            // Save image with item ID
            String filename = imagesStorageService.saveWithItemId(imageFile, createdItem.getId());

            // Update item with image URL
            String imageUrl = "/api/v1/images/" + filename;
            ItemResponse updatedItem = itemService.updateItemImage(createdItem.getId(), imageUrl);

            log.info("Item created successfully with image, ID: {}", updatedItem.getId());

            return ApiResponse.<ItemResponse>builder()
                    .code(201)
                    .message("Item created successfully with image")
                    .result(updatedItem)
                    .build();

        } catch (IllegalArgumentException e) {
            log.error("Validation error creating item with image: {}", e.getMessage());
            return ApiResponse.<ItemResponse>builder()
                    .code(400)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error creating item with image: {}", e.getMessage(), e);
            return ApiResponse.<ItemResponse>builder()
                    .code(500)
                    .message("Failed to create item with image: " + e.getMessage())
                    .build();
        }
    }

    @PutMapping("/{itemId}/image")
    public ApiResponse<Map<String, String>> updateItemImage(@PathVariable Long itemId, @RequestParam("image") MultipartFile imageFile, Authentication authentication) {

        try {
            String sellerId = authentication.getName();
            log.info("Updating image for item {} by seller: {}", itemId, sellerId);

            // Validate image
            if (imageFile.isEmpty()) {
                return ApiResponse.<Map<String, String>>builder()
                        .code(400)
                        .message("Please select an image file")
                        .build();
            }

            if (imagesStorageService.isValidImageType(imageFile)) {
                return ApiResponse.<Map<String, String>>builder()
                        .code(400)
                        .message("Invalid image type. Only JPG, JPEG, PNG, GIF, and WEBP are allowed")
                        .build();
            }

            // Save image with item ID
            String filename = imagesStorageService.saveWithItemId(imageFile, itemId);
            String imageUrl = "/api/v1/images/" + filename;

            // Update item with new image URL
            itemService.updateItemImage(itemId, imageUrl);

            Map<String, String> result = new HashMap<>();
            result.put("filename", filename);
            result.put("imageUrl", imageUrl);

            log.info("Item image updated successfully for item: {}", itemId);

            return ApiResponse.<Map<String, String>>builder()
                    .code(200)
                    .message("Item image updated successfully")
                    .result(result)
                    .build();

        } catch (IllegalArgumentException e) {
            log.error("Validation error updating item image: {}", e.getMessage());
            return ApiResponse.<Map<String, String>>builder()
                    .code(400)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Error updating item image: {}", e.getMessage(), e);
            return ApiResponse.<Map<String, String>>builder()
                    .code(500)
                    .message("Failed to update item image: " + e.getMessage())
                    .build();
        }
    }
}
