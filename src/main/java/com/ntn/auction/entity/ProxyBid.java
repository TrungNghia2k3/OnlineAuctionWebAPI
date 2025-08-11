package com.ntn.auction.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "proxy_bids", indexes = {
        @Index(name = "idx_proxy_bid_item_id", columnList = "item_id"),
        @Index(name = "idx_proxy_bid_user_id", columnList = "user_id"),
        @Index(name = "idx_proxy_bid_status", columnList = "status")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ProxyBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_proxy_bid_item"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_proxy_bid_user"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    User user;

    @Column(name = "max_amount", nullable = false, precision = 19, scale = 4)
    BigDecimal maxAmount;

    @Column(name = "current_amount", precision = 19, scale = 4)
    BigDecimal currentAmount;

    @Column(name = "increment_amount", nullable = false, precision = 19, scale = 4)
    BigDecimal incrementAmount;

    @Column(name = "created_date", nullable = false)
    LocalDateTime createdDate;

    @Column(name = "last_bid_date")
    LocalDateTime lastBidDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    // Using EnumType.STRING to store the enum as a string in the database
    // and columnDefinition to ensure the default value is 'ACTIVE'
    @Builder.Default
    ProxyBidStatus status = ProxyBidStatus.ACTIVE;

    @Builder.Default
    @Column(name = "is_winning", nullable = false)
    Boolean winning = false;

    public enum ProxyBidStatus {
        ACTIVE,     // Hiện tại đang hoạt động
        EXHAUSTED,  // Đã sử dụng hết số tiền tối đa
        OUTBID,     // Đã bị vượt giá bởi một người dùng khác
        WON,        // Đã thắng cuộc đấu giá
        CANCELLED   // Đã bị hủy
    }

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }
}
