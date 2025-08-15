package com.ntn.auction.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ntn.auction.dto.request.ItemCreateRequest;
import com.ntn.auction.dto.response.ApiResponse;
import com.ntn.auction.dto.response.ItemResponse;
import com.ntn.auction.service.ImagesStorageService;
import com.ntn.auction.service.ItemService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ItemController {

    ItemService itemService;
    ImagesStorageService imagesStorageService;
    Validator validator;

    /**
     * Create new auction item with multiple images
     *
     * @param requestJson JSON string containing ItemCreateRequest data
     * @param mainImage   Main image file (required)
     * @param subImages   List of sub images (optional)
     * @return ApiResponse containing created ItemResponse with images
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ItemResponse>> createItem(
            @RequestPart("request") String requestJson,
            @RequestPart("imagesMain") MultipartFile mainImage,
            @RequestPart(value = "imagesSub", required = false) List<MultipartFile> subImages) throws JsonProcessingException {

        log.info("Received item creation request with main image: {}, sub images count: {}",
                mainImage.getOriginalFilename(), subImages != null ? subImages.size() : 0);

        // Validate that main image is provided
        if (mainImage.isEmpty()) {
            throw new IllegalArgumentException("Main image is required");
        }

        // Validate image types
        if (imagesStorageService.isValidImageType(mainImage)) {
            throw new IllegalArgumentException("Invalid main image type. Only JPEG, PNG, GIF, and WebP are allowed");
        }

        if (subImages != null) {
            for (MultipartFile subImage : subImages) {
                if (!subImage.isEmpty() && imagesStorageService.isValidImageType(subImage)) {
                    throw new IllegalArgumentException("Invalid sub image type. Only JPEG, PNG, GIF, and WebP are allowed");
                }
            }
        }

        // Deserialize the JSON request into ItemCreateRequest object
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        ItemCreateRequest request = objectMapper.readValue(requestJson, ItemCreateRequest.class);

        // Validate request data
        Set<ConstraintViolation<ItemCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        try {
            // Create item with images using the new service method
            ItemResponse itemResponse = itemService.createItemWithImages(request, mainImage, subImages);

            log.info("Successfully created item {} with {} images", itemResponse.getId(),
                    itemResponse.getImages() != null ? itemResponse.getImages().size() : 0);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.<ItemResponse>builder()
                            .code(201)
                            .message("Item created successfully with images")
                            .result(itemResponse)
                            .build());

        } catch (Exception e) {
            log.error("Error creating item: {}", e.getMessage(), e);
            throw e; // Let GlobalExceptionHandler handle it
        }
    }

    /**
     * Get item by ID
     *
     * @param id Item ID
     * @return ApiResponse containing ItemResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemResponse>> getItem(@PathVariable Long id) {
        ItemResponse itemResponse = itemService.getById(id);

        return ResponseEntity.ok(ApiResponse.<ItemResponse>builder()
                .code(200)
                .message("Item retrieved successfully")
                .result(itemResponse)
                .build());
    }

    /**
     * Get all items
     *
     * @return ApiResponse containing list of ItemResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getAllItems() {
        List<ItemResponse> items = itemService.getAll();

        return ResponseEntity.ok(ApiResponse.<List<ItemResponse>>builder()
                .code(200)
                .message("Items retrieved successfully")
                .result(items)
                .build());
    }

    /**
     * Get items by seller
     *
     * @param sellerId Seller ID
     * @return ApiResponse containing list of ItemResponse
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getItemsBySeller(@PathVariable String sellerId) {
        List<ItemResponse> items = itemService.getItemsBySeller(sellerId);

        return ResponseEntity.ok(ApiResponse.<List<ItemResponse>>builder()
                .code(200)
                .message("Items retrieved successfully")
                .result(items)
                .build());
    }

    /**
     * Get items by category
     *
     * @param categoryId Category ID
     * @return ApiResponse containing list of ItemResponse
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getItemsByCategory(@PathVariable Long categoryId) {
        List<ItemResponse> items = itemService.getItemsByCategory(categoryId);

        return ResponseEntity.ok(ApiResponse.<List<ItemResponse>>builder()
                .code(200)
                .message("Items retrieved successfully")
                .result(items)
                .build());
    }

    /**
     * Get active auctions
     *
     * @return ApiResponse containing list of ItemResponse
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getActiveAuctions() {
        List<ItemResponse> items = itemService.getActiveAuctions();

        return ResponseEntity.ok(ApiResponse.<List<ItemResponse>>builder()
                .code(200)
                .message("Active auctions retrieved successfully")
                .result(items)
                .build());
    }

    /**
     * Get auctions ending soon
     *
     * @return ApiResponse containing list of ItemResponse
     */
    @GetMapping("/ending-soon")
    public ResponseEntity<ApiResponse<List<ItemResponse>>> getEndingSoonAuctions() {
        List<ItemResponse> items = itemService.getEndingSoonAuctions();

        return ResponseEntity.ok(ApiResponse.<List<ItemResponse>>builder()
                .code(200)
                .message("Ending soon auctions retrieved successfully")
                .result(items)
                .build());
    }
}
