package com.ntn.auction.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImagesStorageService {
    String itemImageDirectory = "images/items/";
    Path root = Paths.get(itemImageDirectory);

    public void init() {
        try {
            Files.createDirectories(root);
            log.info("Image storage directory initialized: {}", root.toAbsolutePath());
        } catch (IOException e) {
            log.error("Could not initialize folder for upload!", e);
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    /**
     * Save image file with custom filename pattern: item-{itemId}-{timestamp}.{extension}
     * @param file MultipartFile to save
     * @param itemId The item ID for filename generation
     * @return Generated filename for database storage
     */
    public String saveWithItemId(MultipartFile file, Long itemId) {
        try {
            String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
            String extension = getFileExtension(originalFilename);
            long timestamp = System.currentTimeMillis();
            String generatedFilename = String.format("item-%d-%d.%s", itemId, timestamp, extension);

            Path targetLocation = this.root.resolve(generatedFilename);
            Files.copy(file.getInputStream(), targetLocation);

            log.info("Image saved successfully: {}", generatedFilename);
            return generatedFilename;
        } catch (Exception e) {
            if (e instanceof FileAlreadyExistsException) {
                log.error("A file of that name already exists: {}", e.getMessage());
                throw new RuntimeException("A file of that name already exists.");
            }
            log.error("Error saving image file: {}", e.getMessage());
            throw new RuntimeException("Could not save image file: " + e.getMessage());
        }
    }

    /**
     * Save image file with original filename (for general uploads)
     * @param file MultipartFile to save
     * @return Original filename
     */
    public String save(MultipartFile file) {
        try {
            String filename = Objects.requireNonNull(file.getOriginalFilename());
            Files.copy(file.getInputStream(), this.root.resolve(filename));

            log.info("Image saved successfully: {}", filename);
            return filename;
        } catch (Exception e) {
            if (e instanceof FileAlreadyExistsException) {
                log.error("A file of that name already exists: {}", e.getMessage());
                throw new RuntimeException("A file of that name already exists.");
            }
            log.error("Error saving image file: {}", e.getMessage());
            throw new RuntimeException("Could not save image file: " + e.getMessage());
        }
    }

    public Resource load(String filename) {
        try {
            Path file = root.resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.debug("Image loaded successfully: {}", filename);
                return resource;
            } else {
                log.error("Could not read the file: {}", filename);
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            log.error("Error loading image file: {}", e.getMessage());
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }

    /**
     * Delete image file from storage
     * @param filename Filename to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean delete(String filename) {
        try {
            Path file = root.resolve(filename);
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                log.info("Image deleted successfully: {}", filename);
            } else {
                log.warn("Image file not found for deletion: {}", filename);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Error deleting image file: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if image file exists
     * @param filename Filename to check
     * @return true if exists, false otherwise
     */
    public boolean exists(String filename) {
        Path file = root.resolve(filename);
        return Files.exists(file);
    }

    /**
     * Get file extension from filename
     * @param filename Full filename
     * @return File extension without dot
     */
    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // No extension
        }
        return filename.substring(lastIndexOf + 1).toLowerCase();
    }

    /**
     * Validate image file type
     * @param file MultipartFile to validate
     * @return true if valid image type
     */
    public boolean isValidImageType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null || (!contentType.equals("image/jpeg") &&
                                       !contentType.equals("image/jpg") &&
                                       !contentType.equals("image/png") &&
                                       !contentType.equals("image/gif") &&
                                       !contentType.equals("image/webp"));
    }
}
