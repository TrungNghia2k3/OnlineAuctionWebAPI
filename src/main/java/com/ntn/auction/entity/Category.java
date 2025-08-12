package com.ntn.auction.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Index để làm gì?
// Index giúp tăng tốc độ truy vấn cơ sở dữ liệu bằng cách tạo ra các cấu trc dữ liệu phụ trợ.
// Trong trường hợp này, các index được tạo trên các cột thường xuyên được truy vấn
// hoặc lọc trong các truy vấn SQL, như `name` và `is_active`.
// Điều này giúp cải thiện hiệu suất truy vấn khi tìm kiếm các bản ghi
// dựa trên các cột này, giảm thiểu thời gian cần thiết để tìm kiếm
// và truy xuất dữ liệu từ bảng `category`.

@Entity
@Table(name = "category", indexes = {
        @Index(name = "idx_category_name", columnList = "name"),
        @Index(name = "idx_category_active", columnList = "is_active")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = false) // Không so sánh với các trường của lớp cha (nếu có)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    String name;

    @Column(name = "description", length = 500)
    @Lob // Sử dụng @Lob để lưu trữ văn bản dài trong cơ sở dữ liệu
    String description;

    @Column(name = "image_url", length = 500)
    String imageUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    // Sử dụng columnDefinition để đảm bảo giá trị mặc định là true
    boolean active = true;

    // Make these nullable initially to avoid migration issues
    @Column(name = "created_date", updatable = false)
    LocalDateTime createdDate;

    @Column(name = "updated_date")
    LocalDateTime updatedDate;

    @Column(name = "min_starting_price", precision = 19, scale = 4, columnDefinition = "DECIMAL(19,4) DEFAULT 1.00")
    BigDecimal minStartingPrice;

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
