package com.ntn.auction.service;

import com.ntn.auction.dto.request.ItemCreateRequest;
import com.ntn.auction.dto.response.ItemResponse;
import com.ntn.auction.entity.Category;
import com.ntn.auction.entity.Item;
import com.ntn.auction.entity.ItemImage;
import com.ntn.auction.entity.User;
import com.ntn.auction.exception.CategoryNotFoundException;
import com.ntn.auction.exception.ItemNotFoundException;
import com.ntn.auction.exception.UserNotFoundException;
import com.ntn.auction.mapper.ItemMapper;
import com.ntn.auction.repository.CategoryRepository;
import com.ntn.auction.repository.ItemRepository;
import com.ntn.auction.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemService {

    ItemRepository itemRepository;
    CategoryRepository categoryRepository;
    UserRepository userRepository;
    ItemMapper itemMapper;
    BidIncrementService bidIncrementService;
    ImagesStorageService imagesStorageService;

    private static final String BASE_IMAGE_URL = "http://localhost:8080/api/v1/";

    private ItemResponse createItem(ItemCreateRequest request) {
        // Validate seller
        User seller = userRepository.findById(request.getSellerId())
                .orElseThrow(() -> new UserNotFoundException("Seller not found: " + request.getSellerId()));

        // Validate category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException("Category not found: " + request.getCategoryId()));

        // Validate starting price against category minimum
        if (request.getStartingPrice().compareTo(category.getMinStartingPrice()) < 0) {
            throw new IllegalArgumentException("Starting price (" + request.getStartingPrice() +
                                               ") must be greater than or equal to category minimum (" + category.getMinStartingPrice() + ")");
        }

        // Validate auction dates
        if (request.getAuctionEndDate().isBefore(request.getAuctionStartDate())) {
            throw new IllegalArgumentException("Auction end date must be after start date");
        }

        if (request.getAuctionStartDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Auction start date must be in the future");
        }

        // Calculate initial minimum bid increment
        BigDecimal initialMinIncrement = bidIncrementService.calculateMinIncrement(request.getStartingPrice());

        // Create item entity
        Item item = Item.builder()
                .name(request.getName())
                .description(request.getDescription())
                .minIncreasePrice(initialMinIncrement)
                .currentBidPrice(request.getStartingPrice())
                .auctionStartDate(request.getAuctionStartDate())
                .auctionEndDate(request.getAuctionEndDate())
                .originalEndDate(request.getAuctionEndDate()) // Store original end date
                .reservePrice(request.getReservePrice())
                .startingPrice(request.getStartingPrice())
                .category(category)
                .seller(seller)
                .build();


        Item savedItem = itemRepository.save(item);
        log.info("Created new item with ID: {} for seller: {}", savedItem.getId(), seller.getId());

        return itemMapper.mapToItemResponse(savedItem);
    }

    /**
     * Create item with multiple images
     *
     * @param request   ItemCreateRequest
     * @param mainImage Main image file
     * @param subImages List of sub images
     * @return ItemResponse with images included
     */
    @PreAuthorize("hasRole('USER')")
    @Transactional
    public ItemResponse createItemWithImages(ItemCreateRequest request, MultipartFile mainImage, List<MultipartFile> subImages) {
        // Create item first to get the ID
        ItemResponse itemResponse = createItem(request);

        // Get the actual item entity from database
        Item item = itemRepository.findById(itemResponse.getId())
                .orElseThrow(() -> new ItemNotFoundException("Item not found: " + itemResponse.getId()));

        // Save images using the item ID and Item entity - this will properly set the foreign key
        List<ItemImage> savedImages = imagesStorageService.saveItemImages(mainImage, subImages, itemResponse.getId(), item);

        // The ItemImage entities are already saved in ImagesStorageService with proper item association
        // No need to save the item again - just update the response with images
        itemResponse.setImages(savedImages);

        log.info("Created item {} with {} images", itemResponse.getId(), savedImages.size());
        return itemResponse;
    }

    public List<ItemResponse> getAll() {
        List<Item> items = itemRepository.findAll();
        return itemMapper.mapToItemResponseList(items);
    }

    public ItemResponse getById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found: " + id));
        return mapItemResponse(item);
    }

    public List<ItemResponse> getItemsBySeller(String sellerId) {
        List<Item> items = itemRepository.findBySellerIdOrderByAuctionStartDateDesc(sellerId);
        return itemMapper.mapToItemResponseList(items);
    }

    public List<ItemResponse> getItemsByCategory(Long categoryId) {
        List<Item> items = itemRepository.findByCategoryIdAndStatusOrderByAuctionEndDateAsc(categoryId, Item.ItemStatus.ACTIVE);
        return itemMapper.mapToItemResponseList(items);
    }

    @Transactional
    public void updateItemStatus(Long itemId, Item.ItemStatus status) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ItemNotFoundException("Item not found: " + itemId));

        item.setStatus(status);
        itemRepository.save(item);

        log.info("Updated item {} status to: {}", itemId, status);
    }

    public List<ItemResponse> getActiveAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Item> activeItems = itemRepository.findActiveAuctions(now);
        return itemMapper.mapToItemResponseList(activeItems);
    }

    public List<ItemResponse> getEndingSoonAuctions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next24Hours = now.plusHours(24);
        List<Item> endingSoonItems = itemRepository.findEndingSoonAuctions(now, next24Hours);
        return itemMapper.mapToItemResponseList(endingSoonItems);
    }

    private ItemResponse mapItemResponse(Item item) {
        ItemResponse response = itemMapper.mapToItemResponse(item);

        // Gắn BASE_IMAGE_URL vào từng ảnh
        if (response.getImages() != null) {
            response.getImages().forEach(img ->
                    img.setImageUrl(BASE_IMAGE_URL + img.getImageUrl())
            );
        }

        return response;
    }
}
