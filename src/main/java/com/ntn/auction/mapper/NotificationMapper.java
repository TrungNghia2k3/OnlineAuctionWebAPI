package com.ntn.auction.mapper;

import com.ntn.auction.dto.request.NotificationCreateRequest;
import com.ntn.auction.dto.response.NotificationResponse;
import com.ntn.auction.entity.Item;
import com.ntn.auction.entity.Notification;
import com.ntn.auction.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(target = "user", expression = "java(toUser(request.getUserId()))")
    @Mapping(target = "item", expression = "java(toItem(request.getItemId()))")
    @Mapping(target = "notificationDate", expression = "java(java.time.LocalDateTime.now())")
    Notification toEntity(NotificationCreateRequest request);

    // Helper methods để map thủ công
    default User toUser(String userId) {
        if (userId == null) return null;
        User user = new User();
        user.setId(userId);
        return user;
    }

    default Item toItem(Long itemId) {
        if (itemId == null) return null;
        Item item = new Item();
        item.setId(itemId);
        return item;
    }

    List<NotificationResponse> toResponseList(List<Notification> entities);
}
