package com.ntn.auction.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    @Lob // Sử dụng @Lob để lưu trữ văn bản dài trong cơ sở dữ liệu
    String description;

    @Column(name = "image_url", length = 100)
    String imageUrl;

    @Column(name = "min_increase_price", nullable = false, precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0.0000")
    BigDecimal minIncreasePrice;

    @Column(name = "current_bid_price", precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0.0000")
    // Phải là 0.0000 để đảm bảo tính chính xác của giá trị tiền tệ vì scale là 4
    BigDecimal currentBidPrice;

    @Column(name = "auction_start_date", columnDefinition = "TIMESTAMP", nullable = false)
    LocalDateTime auctionStartDate;

    @Column(name = "auction_end_date", columnDefinition = "TIMESTAMP", nullable = false)
    LocalDateTime auctionEndDate;

    // Anti-sniping fields - will be added via migration
    @Column(name = "original_end_date", columnDefinition = "TIMESTAMP")
    LocalDateTime originalEndDate;

    @Builder.Default // Sử dụng @Builder.Default để đảm bảo giá trị mặc định khi sử dụng Builder
    @Column(name = "anti_snipe_extension_minutes", columnDefinition = "INTEGER DEFAULT 5")
    Integer antiSnipeExtensionMinutes = 5;

    @Builder.Default // Sử dụng @Builder.Default để đảm bảo giá trị mặc định khi sử dụng Builder
    @Column(name = "anti_snipe_threshold_minutes", columnDefinition = "INTEGER DEFAULT 2")
    Integer antiSnipeThresholdMinutes = 2;

    @Builder.Default
    @Column(name = "max_extensions", columnDefinition = "INTEGER DEFAULT 3")
    Integer maxExtensions = 3;

    @Builder.Default
    @Column(name = "current_extensions", columnDefinition = "INTEGER DEFAULT 0")
    Integer currentExtensions = 0;

    // Reserve price fields - will be added via migration
    @Column(name = "reserve_price", precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0.00")
    BigDecimal reservePrice;

    @Column(name = "starting_price", precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 0.00")
    BigDecimal startingPrice;

    @Builder.Default
    @Column(name = "is_reserve_met")
    // Sử dụng columnDefinition để đảm bảo giá trị mặc định là false
    // Điều này giúp tránh việc phải kiểm tra null trong mã và đảm bảo tính nhất quán
    // của dữ liệu trong cơ sở dữ liệu.
    // Khi sử dụng @Builder.Default, giá trị này sẽ được khởi tạo là false
    // khi tạo đối tượng Item mới thông qua Builder.
    // Điều này giúp đảm bảo rằng khi một Item được tạo ra, trường reserveMet
    // sẽ luôn có giá trị mặc định là false, trừ khi được chỉ định khác
    // trong quá trình khởi tạo.
    Boolean reserveMet = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Sử dụng columnDefinition để đảm bảo giá trị mặc định là 'PENDING'
    @Builder.Default
    ItemStatus status = ItemStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_category"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false, foreignKey = @ForeignKey(name = "fk_item_seller"))
    @ToString.Exclude // Loại trừ trường này khỏi phương thức toString để tránh vòng lặp vô hạn
    @EqualsAndHashCode.Exclude // Ngăn vòng lặp vô hạn trong JSON serialization
    User seller;

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
