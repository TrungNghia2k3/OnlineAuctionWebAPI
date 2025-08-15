package com.ntn.auction.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bid", indexes = {
        @Index(name = "idx_bid_item_id", columnList = "item_id"),
        @Index(name = "idx_bid_buyer_id", columnList = "buyer_id"),
        @Index(name = "idx_bid_status", columnList = "status"),
        @Index(name = "idx_bid_time", columnList = "bid_time")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    BigDecimal amount;

    @Column(name = "bid_time", nullable = false, columnDefinition = "TIMESTAMP")
    LocalDateTime bidTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    BidStatus status = BidStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bid_item"))
    @JsonBackReference("item-bids")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bid_buyer"))
    @JsonBackReference("user-bids")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User buyer;

    @Builder.Default
    @Column(name = "is_highest_bid", nullable = false)
    Boolean highestBid = false;

    @Builder.Default
    @Column(name = "is_proxy_bid", nullable = false)
    Boolean proxyBid = false;

    @PrePersist
    protected void onCreate() {
        if (bidTime == null) {
            bidTime = LocalDateTime.now();
        }
    }

    public enum BidStatus {
        PENDING,    // Pending verification
        ACCEPTED,   // Accepted (current highest bid)
        OUTBID,     // Outbid by another user
        WON,        // Won the auction
        LOST,       // Lost the auction
        CANCELLED   // Bid cancelled
    }
}
