package com.ntn.auction.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
// Giống như controller trong Spring MVC, nhưng ở đây nó xử lý WebSocket messages (qua STOMP) thay vì HTTP requests.
//Spring WebSocket sẽ scan các method có @MessageMapping để map message từ client.
public class WebSocketController {

    @MessageMapping("/bid/subscribe/{itemId}")
    @SendToUser("/queue/bid/confirmation")
    public String subscribeToBids(@DestinationVariable Long itemId) {
        return "Subscribed to bids for item " + itemId;
    }

    // 2.1 @MessageMapping("/bid/subscribe/{itemId}")
    //      Tương tự như @RequestMapping trong REST API, nhưng dành cho STOMP messages.
    //      Khi client gửi message tới: /app/bid/subscribe/123
    //      (prefix /app đến từ setApplicationDestinationPrefixes trong WebSocketConfig)
    //      → Spring sẽ gọi method này với itemId = 123.
    //
    // 2.2 @DestinationVariable Long itemId
    //      Giống như @PathVariable trong REST API.
    //      Lấy giá trị {itemId} từ đường dẫn message STOMP.
    //Ví dụ: /app/bid/subscribe/5 → itemId = 5.
    //
    //2.3 @SendToUser("/queue/bid/confirmation")
    //      Sau khi method xử lý xong, kết quả trả về sẽ được gửi chỉ tới user đã gửi request này.
    //      Điểm đến là /user/queue/bid/confirmation (Spring sẽ tự thêm /user ở đầu).
    //      Điều này đảm bảo đây là private message.
    // 📌 Nếu dùng @SendTo("/topic/...") thì tin nhắn sẽ broadcast tới tất cả client subscribe.
}
