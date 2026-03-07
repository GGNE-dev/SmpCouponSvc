package org.ggne.test.coupon.dto;

import lombok.Builder;
import lombok.Getter;
import org.ggne.test.coupon.domain.Coupon;
import org.ggne.test.coupon.domain.DiscountType;

import java.time.LocalDateTime;

@Getter
@Builder
public class CouponResponse {
    private Long couponId;
    private String name;
    private DiscountType discountType;
    private int discountValue;
    private int remainQuantity;
    private LocalDateTime expiredAt;

    public static CouponResponse from(Coupon coupon) {
        return CouponResponse.builder()
                .couponId(coupon.getId())
                .name(coupon.getName())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .remainQuantity(coupon.getTotalQuantity() - coupon.getIssuedQuantity())
                .expiredAt(coupon.getExpiredAt())
                .build();
    }
}
