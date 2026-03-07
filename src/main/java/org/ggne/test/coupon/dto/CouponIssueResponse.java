package org.ggne.test.coupon.dto;

import lombok.Builder;
import lombok.Getter;
import org.ggne.test.coupon.domain.Coupon;
import org.ggne.test.coupon.domain.CouponIssue;

import java.time.LocalDateTime;

@Getter
@Builder
public class CouponIssueResponse {
    private Long couponIssueId;
    private Long couponId;
    private String couponName;
    private String status;
    private LocalDateTime issuedAt;

    public static CouponIssueResponse of(CouponIssue couponIssue, Coupon coupon) {
        return CouponIssueResponse.builder()
                .couponIssueId(couponIssue.getId())
                .couponId(coupon.getId())
                .couponName(coupon.getName())
                .status(couponIssue.getStatus().name())
                .issuedAt(couponIssue.getIssuedAt())
                .build();
    }
}
