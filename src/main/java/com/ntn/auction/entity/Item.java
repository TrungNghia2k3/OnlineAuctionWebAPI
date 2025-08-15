package com.ntn.auction.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item", indexes = {
        @Index(name = "idx_item_category_id", columnList = "category_id"),
        @Index(name = "idx_item_seller_id", columnList = "seller_id"),
        @Index(name = "idx_item_status", columnList = "status"),
        @Index(name = "idx_item_auction_start_date", columnList = "auction_start_date"),
        @Index(name = "idx_item_auction_end_date", columnList = "auction_end_date")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    @Lob
    String description;

    @Column(name = "image_url", length = 100)
    String imageUrl;

    @Column(name = "min_increase_price", nullable = false, precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0.0000")
    BigDecimal minIncreasePrice;

    @Column(name = "current_bid_price", precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0.0000")
    BigDecimal currentBidPrice;

    @Column(name = "auction_start_date", columnDefinition = "TIMESTAMP", nullable = false)
    LocalDateTime auctionStartDate;

    @Column(name = "auction_end_date", columnDefinition = "TIMESTAMP", nullable = false)
    LocalDateTime auctionEndDate;

    @Column(name = "original_end_date", columnDefinition = "TIMESTAMP")
    LocalDateTime originalEndDate;

    @Builder.Default
    @Column(name = "anti_snipe_extension_minutes", columnDefinition = "INTEGER DEFAULT 5")
    Integer antiSnipeExtensionMinutes = 5;

    @Builder.Default
    @Column(name = "anti_snipe_threshold_minutes", columnDefinition = "INTEGER DEFAULT 2")
    Integer antiSnipeThresholdMinutes = 2;

    @Builder.Default
    @Column(name = "max_extensions", columnDefinition = "INTEGER DEFAULT 3")
    Integer maxExtensions = 3;

    @Builder.Default
    @Column(name = "current_extensions", columnDefinition = "INTEGER DEFAULT 0")
    Integer currentExtensions = 0;

    @Column(name = "reserve_price", precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0.00")
    BigDecimal reservePrice;

    @Column(name = "starting_price", precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0.00")
    BigDecimal startingPrice;

    @Builder.Default
    @Column(name = "is_reserve_met")
    Boolean reserveMet = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    ItemStatus status = ItemStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_category"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_seller"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User seller;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    List<ItemImage> images = new ArrayList<>();


    @PrePersist
    protected void onCreate() {
        if (originalEndDate == null) {
            originalEndDate = auctionEndDate;
        }
    }

    public enum ItemStatus {
        PENDING,        // Waiting for admin approval
        APPROVED,       // Approved by admin
        REJECTED,       // Rejected by admin
        UPCOMING,       // Approved but auction hasn't started
        ACTIVE,         // Auction in progress
        SOLD,           // Successfully sold
        EXPIRED,        // Auction ended without buyer
        CANCELLED       // Auction cancelled (only before start)
    }
}
