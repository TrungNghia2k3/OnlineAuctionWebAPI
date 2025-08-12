package com.ntn.auction.controller;

import com.ntn.auction.dto.response.ApiResponse;
import com.ntn.auction.service.ImagesStorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ImageController {

    ImagesStorageService imagesStorageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Map<String, String>>builder()
                                .code(400)
                                .message("Please select a file to upload")
                                .build());
            }

            // Validate image type
            if (imagesStorageService.isValidImageType(file)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Map<String, String>>builder()
                                .code(400)
                                .message("Invalid file type. Only JPG, JPEG, PNG, GIF, and WEBP are allowed")
                                .build());
            }

            // Save file
            String filename = imagesStorageService.save(file);

            Map<String, String> result = new HashMap<>();
            result.put("filename", filename);
            result.put("url", "/api/v1/images/" + filename);

            log.info("Image uploaded successfully: {}", filename);

            return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                    .code(200)
                    .message("Image uploaded successfully")
                    .result(result)
                    .build());

        } catch (RuntimeException e) {
            log.error("Error uploading image: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Map<String, String>>builder()
                            .code(500)
                            .message("Failed to upload image: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        try {
            Resource file = imagesStorageService.load(filename);

            // Determine content type based on file extension
            String contentType = getContentType(filename);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(file);

        } catch (RuntimeException e) {
            log.error("Error loading image {}: {}", filename, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(@PathVariable String filename) {
        try {
            boolean deleted = imagesStorageService.delete(filename);

            if (deleted) {
                log.info("Image deleted successfully: {}", filename);
                return ResponseEntity.ok(ApiResponse.<Void>builder()
                        .code(200)
                        .message("Image deleted successfully")
                        .build());
            } else {
                return ResponseEntity.notFound()
                        .build();
            }

        } catch (Exception e) {
            log.error("Error deleting image {}: {}", filename, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.<Void>builder()
                            .code(500)
                            .message("Failed to delete image: " + e.getMessage())
                            .build());
        }
    }

    @RequestMapping(value = "/{filename}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkImageExists(@PathVariable String filename) {
        boolean exists = imagesStorageService.exists(filename);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}
