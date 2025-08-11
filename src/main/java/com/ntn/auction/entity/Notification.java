package com.ntn.auction.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_item_id", columnList = "item_id"),
        @Index(name = "idx_notification_date", columnList = "notification_date"),
        @Index(name = "idx_notification_read", columnList = "is_read")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @Lob
    String message;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    // Using columnDefinition to ensure the default value is false
    // This ensures that the database will set the default value to false
    Boolean read = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_user"))
    @ToString.Exclude // Exclude this field from toString to avoid circular references
    @EqualsAndHashCode.Exclude // Exclude this field from equals and hashCode to avoid circular references
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_notification_item"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Item item;

    @Column(name = "notification_date", columnDefinition = "TIMESTAMP", nullable = false)
    LocalDateTime notificationDate;

    @PrePersist
    protected void onCreate() {
        if (notificationDate == null) {
            notificationDate = LocalDateTime.now();
        }
    }
}
