package com.ntn.auction.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // Bật hỗ trợ WebSocket với STOMP message broker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // WebSocketMessageBrokerConfigurer interface provides methods to configure the message broker and endpoints
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple memory-based message broker
        config.enableSimpleBroker("/topic", "/queue");
        // - Bật SimpleBroker (message broker chạy trong bộ nhớ RAM) để gửi message tới client.
        // - topic → dành cho broadcast (nhiều client subscribe cùng nhận, kiểu publish-subscribe).
        // - queue → dành cho point-to-point (một client gửi cho một client khác).
        // Ví dụ:
        // - Server gửi tới /topic/chat → tất cả client subscribe /topic/chat nhận được.
        // - Server gửi tới /queue/reply → chỉ client subscribe đúng /queue/reply mới nhận.

        // Prefix for messages bound for methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
        // - Định nghĩa prefix cho các message từ client gửi lên server.
        // - Khi client gửi đến /app/bid, Spring sẽ tìm method @MessageMapping("/bid") để xử lý.
        // - Nó giống như "base path" cho request trong REST API.


        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
        // Đặt prefix cho tin nhắn riêng (private) gửi tới từng user.
        // Khi server gửi tới /user/{username}/queue/messages, chỉ user đó nhận được.
        // Spring sẽ tự map /user thành session của user đã kết nối.
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    // Ý nghĩa:
    // addEndpoint("/ws")
    //  Tạo một endpoint WebSocket tại /ws để client kết nối.
    //  Client sẽ kết nối bằng URL:
    //  ws://localhost:8080/ws (hoặc qua SockJS fallback).
    //setAllowedOriginPatterns("*")
    //  Cho phép mọi domain (CORS) được kết nối WebSocket.
    //.withSockJS()
    //  Bật SockJS fallback → nếu trình duyệt không hỗ trợ WebSocket gốc, nó sẽ dùng XHR, long-polling...
    //  Hữu ích khi cần hỗ trợ các trình duyệt cũ hoặc qua firewall.
}
