package com.ntn.auction.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@EqualsAndHashCode(callSuper = false)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    String name;

    @Column(name = "description", length = 500)
    @Lob
    String description;

    @Column(name = "image_url", length = 500)
    String imageUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    boolean active = true;

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
