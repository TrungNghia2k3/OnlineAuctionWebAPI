package com.ntn.auction.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/images")
@Tag(name = "Image", description = "Endpoints for serving item images")
public class ImageController {

    @GetMapping("/item/{imageName:.+}")
    public ResponseEntity<Resource> getItemImage(@PathVariable String imageName)
            throws MalformedURLException {
        return getImageResource(imageName);
    }

    private ResponseEntity<Resource> getImageResource(String imageName)
            throws MalformedURLException {
        String itemImageDirectory = "images/items/";
        Path imagePath = Paths.get(itemImageDirectory).resolve(imageName);
        Resource resource = new UrlResource(imagePath.toUri());
        if (resource.exists() && resource.isReadable()) {
            String contentType = determineContentType(imageName);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private String determineContentType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        }
        // Mặc định trả về image/jpeg nếu không xác định được định dạng
        return "image/jpeg";
    }
}
