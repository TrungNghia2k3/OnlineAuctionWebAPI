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
    String ipAddress;

    @Column(name = "user_agent", length = 500)
    String userAgent;

    @Column(name = "session_id")
    String sessionId;

    @Builder.Default
    @Column(name = "is_proxy_bid", nullable = false)
    Boolean proxyBid = false;

    @Column(name = "validation_hash", nullable = false, length = 256)
    String validationHash;

    public enum ActionType {
        BID_PLACED,         // Place a new bid
        BID_OUTBID,         // Bid exceeded
        BID_WON,            // Bid won
        BID_CANCELLED,      // Bid canceled
        PROXY_BID_EXECUTED  // Execute proxy bid
    }

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
