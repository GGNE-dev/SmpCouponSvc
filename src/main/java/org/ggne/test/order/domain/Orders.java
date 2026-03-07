package org.ggne.test.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Orders {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long couponIssueId; // 쿠폰 미적용 시 null 가능

    @Column(nullable = false, length = 200)
    private String itemName;

    @Column(nullable = false)
    private int originalPrice;

    @Column(nullable = false)
    private int discountAmount;

    @Column(nullable = false)
    private int finalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime orderedAt;

    @Builder
    public Orders(Long userId, Long couponIssueId, String itemName, int originalPrice, int discountAmount, int finalPrice) {
        this.userId = userId;
        this.couponIssueId = couponIssueId;
        this.itemName = itemName;
        this.originalPrice = originalPrice;
        this.discountAmount = discountAmount;
        this.finalPrice = finalPrice;
        this.status = OrderStatus.PENDING; // 기본값
    }
}
