package com.ntn.auction.service;

import com.ntn.auction.dto.request.ItemCreateRequest;
import com.ntn.auction.dto.response.ItemResponse;
import com.ntn.auction.entity.Category;
import com.ntn.auction.entity.Item;
import com.ntn.auction.entity.User;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public ItemResponse createItem(ItemCreateRequest request, String sellerId) {
        // Validate seller
        User seller = userRepository.findById(sellerId)
            .orElseThrow(() -> new UserNotFoundException("Seller not found: " + sellerId));

        // Validate category
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new IllegalArgumentException("Category not found: " + request.getCategoryId()));

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
            .imageUrl(request.getImageUrl())
            .startingPrice(request.getStartingPrice())
            .currentBidPrice(request.getStartingPrice())
            .minIncreasePrice(initialMinIncrement)
            .auctionStartDate(request.getAuctionStartDate())
            .auctionEndDate(request.getAuctionEndDate())
            .status(Item.ItemStatus.PENDING)
            .category(category)
            .seller(seller)
            .build();

        Item savedItem = itemRepository.save(item);
        log.info("Created new item with ID: {} for seller: {}", savedItem.getId(), sellerId);

        return itemMapper.toResponse(savedItem);
    }

    @Transactional
    public ItemResponse updateItemImage(Long itemId, String imageUrl) {
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new ItemNotFoundException("Item not found: " + itemId));

        item.setImageUrl(imageUrl);

        Item updatedItem = itemRepository.save(item);
        log.info("Updated image for item {}: {}", itemId, imageUrl);

        return itemMapper.toResponse(updatedItem);
    }

    public List<ItemResponse> getAll() {
        List<Item> items = itemRepository.findAll();
        return itemMapper.toResponseList(items);
    }

    public ItemResponse getById(Long id) {
        Item item = itemRepository.findById(id)
            .orElseThrow(() -> new ItemNotFoundException("Item not found: " + id));
        return itemMapper.toResponse(item);
    }

    public List<ItemResponse> getItemsBySeller(String sellerId) {
        List<Item> items = itemRepository.findBySellerIdOrderByAuctionStartDateDesc(sellerId);
        return itemMapper.toResponseList(items);
    }

    public List<ItemResponse> getItemsByCategory(Long categoryId) {
        List<Item> items = itemRepository.findByCategoryIdAndStatusOrderByAuctionEndDateAsc(categoryId, Item.ItemStatus.ACTIVE);
        return itemMapper.toResponseList(items);
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
        return itemMapper.toResponseList(activeItems);
    }

    public List<ItemResponse> getEndingSoonAuctions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next24Hours = now.plusHours(24);
        List<Item> endingSoonItems = itemRepository.findEndingSoonAuctions(now, next24Hours);
        return itemMapper.toResponseList(endingSoonItems);
    }
}
