package com.ntn.auction.service;

import com.ntn.auction.dto.event.BidProcessingEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener for asynchronous bid processing
 * Handles heavy operations in background to keep critical path fast
 */
@Component
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BidEventListenerService {

    BidOptimizedService bidOptimizedService;

    @EventListener
    @Async("bidProcessingExecutor")
    public void handleBidProcessingEvent(BidProcessingEvent event) {
        log.info("Received bid processing event for bid: {}", event.getBidId());

        try {
            bidOptimizedService.processBidInBackground(event);
        } catch (Exception e) {
            log.error("Failed to process bid event for bid {}: {}", event.getBidId(), e.getMessage(), e);
            // Could implement retry logic or dead letter queue here
        }
    }
}
