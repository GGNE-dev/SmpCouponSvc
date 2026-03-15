package org.ggne.test.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ggne.test.common.domain.Money;
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

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "original_price", nullable = false))
    @Column(nullable = false)
    private Money originalPrice;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "discount_amount", nullable = false))
    @Column(nullable = false)
    private Money discountAmount;

    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "final_price", nullable = false))
    @Column(nullable = false)
    private Money finalPrice;

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
        this.originalPrice = new Money(originalPrice);
        this.discountAmount = new Money(discountAmount);
        this.finalPrice = new Money(finalPrice);
        this.status = OrderStatus.PENDING;
    }

    public void pay() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태인 주문만 결제할 수 있습니다.");
        }
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }
}
