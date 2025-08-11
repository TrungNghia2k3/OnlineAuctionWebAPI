package com.ntn.auction.controller;

import com.ntn.auction.dto.response.ApiResponse;
import com.ntn.auction.dto.response.NotificationResponse;
import com.ntn.auction.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Notification", description = "Endpoints for managing user notifications")
public class NotificationController {

    NotificationService notificationService;

    @GetMapping("/{userId}")
    ApiResponse<List<NotificationResponse>> getUserNotifications(@PathVariable("userId") String userId) {
        return ApiResponse.<List<NotificationResponse>>builder().result(notificationService.getUserNotifications(userId)).build();
    }

    @GetMapping("/{userId}/unread")
    ApiResponse<List<NotificationResponse>> getUnreadNotifications(@PathVariable("userId") String userId) {
        return ApiResponse.<List<NotificationResponse>>builder().result(notificationService.getUnreadNotifications(userId)).build();
    }

    @GetMapping("/{userId}/unread/count")
    ApiResponse<Integer> getUnreadCount(@PathVariable("userId") String userId) {
        return ApiResponse.<Integer>builder().result(notificationService.getUnreadCount(userId)).build();
    }
}
