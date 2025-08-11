package com.ntn.auction.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Index để làm gì?
// Index giúp tăng tốc độ truy vấn cơ sở dữ liệu bằng cách tạo ra các cấu trúc dữ liệu phụ trợ.
// Trong trường hợp này, các index được tạo trên các cột thường xuyên được truy vấn
// hoặc lọc trong các truy vấn SQL, như `item_id`, `buyer_id`,
// `status`, và `bid_time`. Điều này giúp cải thiện hiệu suất truy vấn khi
// tìm kiếm các bản ghi dựa trên các cột này, giảm thiểu thời gian
// cần thiết để tìm kiếm và truy xuất dữ liệu từ bảng `bid`.

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
@Builder // Sử dụng @Builder để hỗ trợ tạo đối tượng Bid một cách linh hoạt
@EqualsAndHashCode(callSuper = false) // Không so sánh với các trường của lớp cha (nếu có)
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
    // Sử dụng EnumType.STRING để lưu trữ giá trị enum dưới dạng chuỗi
    // và columnDefinition để đảm bảo giá trị mặc định là 'PENDING'
    @Builder.Default // Sử dụng @Builder.Default để đảm bảo giá trị mặc định khi sử dụng Builder
    BidStatus status = BidStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bid_item"))
    // Thêm foreign key để đảm bảo tính toàn vẹn dữ liệu
    @JsonBackReference("item-bids") // Sử dụng @JsonBackReference để tránh vòng lặp vô hạn trong JSON serialization
    @ToString.Exclude // Loại trừ trường này khỏi phương thức toString để tránh vòng lặp vô hạn
    @EqualsAndHashCode.Exclude // Loại trừ trường này khỏi phương thức equals và hashCode để tránh vòng lặp vô hạn
    Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bid_buyer"))
    @JsonBackReference("user-bids") // Sử dụng @JsonBackReference để tránh vòng lặp vô hạn trong JSON serialization
    @ToString.Exclude // Loại trừ trường này khỏi phương thức toString để tránh vòng lặp vô hạn
    @EqualsAndHashCode.Exclude // Loại trừ trường này khỏi phương thức equals và hashCode để tránh vòng lặp vô hạn
    User buyer;

    @Builder.Default // Sử dụng @Builder.Default để đảm bảo giá trị mặc định khi sử dụng Builder
    @Column(name = "is_highest_bid", nullable = false)
    Boolean highestBid = false;

    @Builder.Default // Sử dụng @Builder.Default để đảm bảo giá trị mặc định khi sử dụng Builder
    @Column(name = "is_proxy_bid", nullable = false)
    Boolean proxyBid = false;

    @PrePersist // Phương thức này sẽ được gọi trước khi thực hiện lệnh INSERT vào cơ sở dữ liệu
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
