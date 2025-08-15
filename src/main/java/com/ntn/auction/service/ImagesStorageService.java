package com.ntn.auction.service;

import com.ntn.auction.entity.Item;
import com.ntn.auction.entity.ItemImage;
import com.ntn.auction.repository.ItemImageRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImagesStorageService {
    String itemImageDirectory = "images/items/";
    Path root = Paths.get(itemImageDirectory);
    ItemImageRepository itemImageRepository;

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

    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // No extension
        }
        return filename.substring(lastIndexOf + 1).toLowerCase();
    }

    public boolean isValidImageType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null || (!contentType.equals("image/jpeg") &&
                                       !contentType.equals("image/jpg") &&
                                       !contentType.equals("image/png") &&
                                       !contentType.equals("image/gif") &&
                                       !contentType.equals("image/webp"));
    }


    @Transactional
    public List<ItemImage> saveItemImages(MultipartFile mainImage, List<MultipartFile> subImages, Long itemId, Item item) {
        List<ItemImage> savedImages = new ArrayList<>();

        // Create item-specific directory ONCE
        Path itemDirectory = root.resolve(String.valueOf(itemId));
        try {
            Files.createDirectories(itemDirectory);
            log.info("Created directory for item {}: {}", itemId, itemDirectory.toAbsolutePath());
        } catch (IOException e) {
            log.error("Could not create directory for item {}: {}", itemId, e.getMessage());
            throw new RuntimeException("Could not create directory for item images: " + e.getMessage());
        }

        // Save main image
        if (mainImage != null && !mainImage.isEmpty()) {
            String mainImageFilename = saveImageToItemDirectory(mainImage, itemId, itemDirectory, "main");
            ItemImage mainItemImage = ItemImage.builder()
                    .imageUrl("images/items/" + itemId + "/" + mainImageFilename)
                    .type(ItemImage.ImageType.MAIN)
                    .item(item) // Associate with Item entity
                    .build();
            savedImages.add(mainItemImage);
            log.info("Saved main image for item {}: {}", itemId, mainImageFilename);
        }

        // Save sub images
        if (subImages != null && !subImages.isEmpty()) {
            for (int i = 0; i < subImages.size(); i++) {
                MultipartFile subImage = subImages.get(i);
                if (!subImage.isEmpty()) {
                    String subImageFilename = saveImageToItemDirectory(subImage, itemId, itemDirectory, "sub-" + (i + 1));
                    ItemImage subItemImage = ItemImage.builder()
                            .imageUrl("images/items/" + itemId + "/" + subImageFilename)
                            .type(ItemImage.ImageType.SUB)
                            .item(item) // Associate with Item entity
                            .build();
                    savedImages.add(subItemImage);
                    log.info("Saved sub image {} for item {}: {}", i + 1, itemId, subImageFilename);
                }
            }
        }

        // Save all ItemImage entities to database at once
        List<ItemImage> persistedImages = itemImageRepository.saveAll(savedImages);
        log.info("Successfully saved {} images for item {}", persistedImages.size(), itemId);
        return persistedImages;
    }

    private String saveImageToItemDirectory(MultipartFile file, Long itemId, Path itemDirectory, String imageType) {
        try {
            String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
            String extension = getFileExtension(originalFilename);
            String baseFilename = getBaseFilename(originalFilename);

            // Create slugified filename: item-{itemId}-{imageType}-{slugifiedName}-{timestamp}.{extension}
            String slugifiedName = slugify(baseFilename);
            long timestamp = System.currentTimeMillis();
            String generatedFilename = String.format("item-%d-%s-%s-%d.%s",
                    itemId, imageType, slugifiedName, timestamp, extension);

            Path targetLocation = itemDirectory.resolve(generatedFilename);
            Files.copy(file.getInputStream(), targetLocation);

            log.debug("Image saved: {}", generatedFilename);
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

    private String slugify(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "image";
        }

        // Normalize and remove diacritics
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        String withoutDiacritics = Pattern.compile("\\p{M}").matcher(normalized).replaceAll("");

        // Convert to lowercase, replace spaces and special chars with hyphens, remove consecutive hyphens
        return withoutDiacritics
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-+", "-");
    }

    private String getBaseFilename(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return filename;
        }
        return filename.substring(0, lastIndexOf);
    }

    public Resource loadImageAsResource(String fileName) {
        try {
            Path filePath = root.resolve(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                log.error("Could not read file: {}", fileName);
                throw new RuntimeException("Could not read file: " + fileName);
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL for file: {}", fileName);
            throw new RuntimeException("Malformed URL for file: " + fileName, e);
        } catch (Exception e) {
            log.error("Error loading image file: {}", e.getMessage());
            throw new RuntimeException("Error loading image file: " + e.getMessage());
        }
    }

    public byte[] loadImage(String fileName) {
        try {
            Path filePath = root.resolve(fileName);
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Error reading image file: {}", e.getMessage());
            throw new RuntimeException("Error reading image file: " + e.getMessage());
        }
    }

    /**
     * Load item image resource by item ID and image name
     * Handles the complete path construction and resource loading for item images
     *
     * @param itemId   the ID of the item
     * @param imageName the name of the image file
     * @return Resource representing the image file
     * @throws RuntimeException if image not found or not readable
     */
    public Resource loadItemImage(String itemId, String imageName) throws MalformedURLException {
        // Construct the complete file path: images/items/{itemId}/{imageName}
        Path imagePath = root.resolve(itemId).resolve(imageName);

        // Convert to absolute path and create resource
        Path absolutePath = imagePath.toAbsolutePath();
        Resource resource = new UrlResource(absolutePath.toUri());

        // Log the path being accessed for debugging
        log.debug("Attempting to load item image from path: {}", absolutePath);

        if (resource.exists() && resource.isReadable()) {
            log.info("Successfully loaded item image: {} for item: {}", imageName, itemId);
            return resource;
        } else {
            log.warn("Item image not found or not readable: {} for item: {}", imageName, itemId);
            throw new RuntimeException("Item image not found: " + imageName + " for item: " + itemId);
        }
    }

    /**
     * Load general image resource by filename
     * Used for images stored directly in the root images directory
     *
     * @param filename the name of the image file
     * @return Resource representing the image file
     * @throws RuntimeException if image not found or not readable
     */
    public Resource load(String filename) {
        try {
            Path filePath = root.resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                log.debug("Successfully loaded image: {}", filename);
                return resource;
            } else {
                log.warn("Image not found or not readable: {}", filename);
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL for file: {}", filename);
            throw new RuntimeException("Malformed URL for file: " + filename, e);
        }
    }

    /**
     * Delete image file
     *
     * @param filename the name of the file to delete
     * @return true if file was successfully deleted, false otherwise
     */
    public boolean delete(String filename) {
        try {
            Path filePath = root.resolve(filename);
            boolean deleted = Files.deleteIfExists(filePath);

            if (deleted) {
                log.info("Image deleted successfully: {}", filename);
            } else {
                log.warn("Image file not found for deletion: {}", filename);
            }

            return deleted;
        } catch (IOException e) {
            log.error("Error deleting image file {}: {}", filename, e.getMessage());
            throw new RuntimeException("Could not delete file: " + filename, e);
        }
    }

    /**
     * Check if image file exists
     *
     * @param filename the name of the file to check
     * @return true if file exists and is readable, false otherwise
     */
    public boolean exists(String filename) {
        try {
            Path filePath = root.resolve(filename);
            return Files.exists(filePath) && Files.isReadable(filePath);
        } catch (Exception e) {
            log.warn("Error checking file existence for {}: {}", filename, e.getMessage());
            return false;
        }
    }

    /**
     * Determine content type based on file extension
     * Used for setting proper HTTP content-type headers
     *
     * @param fileName the name of the file
     * @return the MIME type string
     */
    public String determineContentType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        // Default to image/jpeg if extension not recognized
        return "image/jpeg";
    }

    /**
     * Check if an item image exists
     *
     * @param itemId   the ID of the item
     * @param imageName the name of the image file
     * @return true if image exists and is readable, false otherwise
     */
    public boolean itemImageExists(String itemId, String imageName) {
        try {
            Path imagePath = root.resolve(itemId).resolve(imageName);
            Path absolutePath = imagePath.toAbsolutePath();
            return Files.exists(absolutePath) && Files.isReadable(absolutePath);
        } catch (Exception e) {
            log.warn("Error checking item image existence: {}", e.getMessage());
            return false;
        }
    }
}

