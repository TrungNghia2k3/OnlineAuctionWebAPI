package com.ntn.auction.service;

import com.ntn.auction.dto.response.ItemResponse;
import com.ntn.auction.entity.Item;
import com.ntn.auction.mapper.ItemMapper;
import com.ntn.auction.repository.ItemRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ItemService {
    ItemRepository itemRepository;
    ItemMapper itemMapper;

    public List<ItemResponse> getAll() {
        var items = itemRepository.findAll();
        return items.stream()
                .map(this::mapToItemResponse)
                .toList();
    }

    public ItemResponse getById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item not found"));
        return mapToItemResponse(item);
    }

    private ItemResponse mapToItemResponse(Item item) {
        ItemResponse itemResponse = itemMapper.toItemResponse(item);
        itemResponse.setImageUrl(buildImageUrl(item.getImageUrl()));
        return itemResponse;
    }

    private String buildImageUrl(String imageName) {
        String BASE_IMAGE_URL = "http://localhost:8080/api/v1/images/item";
        return BASE_IMAGE_URL + "/" + imageName;
    }
}
