package com.ntn.auction.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bid_audit_log", indexes = {
        @Index(name = "idx_bid_audit_bid_id", columnList = "bid_id"),
        @Index(name = "idx_bid_audit_item_id", columnList = "item_id"),
        @Index(name = "idx_bid_audit_timestamp", columnList = "timestamp")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class BidAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "bid_id", nullable = false)
    Long bidId;

    @Column(name = "item_id", nullable = false)
    Long itemId;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(name = "bid_amount", nullable = false, precision = 19, scale = 4)
    BigDecimal bidAmount;

    @Column(name = "previous_amount", precision = 19, scale = 4)
    BigDecimal previousAmount;

    @Column(name = "timestamp", nullable = false)
    LocalDateTime timestamp;

    @Column(name = "action_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    ActionType actionType;

    @Column(name = "ip_address", length = 45)
    String ipAddress; // Độ dài 45 để hỗ trợ cả IPv4 và IPv6

    @Column(name = "user_agent", length = 500)
    String userAgent; // Độ dài 500 để chứa thông tin User-Agent đầy đủ

    @Column(name = "session_id")
    String sessionId; // ID phiên làm việc của người dùng

    @Builder.Default
    @Column(name = "is_proxy_bid", nullable = false)
    Boolean proxyBid = false;

    @Column(name = "validation_hash", nullable = false, length = 256)
    String validationHash; // Mã băm để xác thực tính toàn vẹn của bản ghi

    public enum ActionType {
        BID_PLACED,         // Đặt giá thầu mới
        BID_OUTBID,         // Giá thầu bị vượt
        BID_WON,            // Giá thầu thắng
        BID_CANCELLED,      // Giá thầu bị hủy
        PROXY_BID_EXECUTED  // Thực hiện giá thầu proxy
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
