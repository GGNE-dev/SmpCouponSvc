package org.ggne.test.coupon.domain;

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
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    // Money의 amount 필드가 INT 타입으로 매핑 됨.
    @Embedded
    @AttributeOverride(name = "amount", column = @Column(name = "discount_value", nullable = false))
    private Money discountValue; // 정액: 원, 정률: %

    @Column(nullable = false)
    private int totalQuantity; // 전체 발급 가능 수량

    @Column(nullable = false)
    private int issuedQuantity; // 현재 발급된 수량

    @Column(nullable = false)
    private LocalDateTime expiredAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Coupon(String name, DiscountType discountType, int discountValue, int totalQuantity, LocalDateTime expiredAt) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = new Money(discountValue);
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.expiredAt = expiredAt;
    }

    // 수량 증가 (A- 버전: 단순 증가)
    public void incrementIssuedQuantity() {
        this.issuedQuantity++;
    }

    // 발급 가능 여부 확인
    public boolean isIssueable() {
        return issuedQuantity < totalQuantity && expiredAt.isAfter(LocalDateTime.now());
    }

    // 할인 금액 계산
    public Money calculateDiscount(Money originalPrice) {
        return switch (this.getDiscountType()) {
            case FIXED -> this.getDiscountValue();
            case RATE -> originalPrice.multiplyByRate(this.discountValue.getAmount());
        };
    }
}
