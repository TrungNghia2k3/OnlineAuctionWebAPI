package com.ntn.auction.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    @MessageMapping("/bid/subscribe/{itemId}")
    @SendToUser("/queue/bid/confirmation")
    public String subscribeToBids(@DestinationVariable Long itemId) {
        return "Subscribed to bids for item " + itemId;
    }
}
