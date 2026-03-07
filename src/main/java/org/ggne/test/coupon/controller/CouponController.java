package org.ggne.test.coupon.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ggne.test.common.response.ApiResponse;
import org.ggne.test.coupon.domain.Coupon;
import org.ggne.test.coupon.domain.CouponIssue;
import org.ggne.test.coupon.dto.CouponCreateRequest;
import org.ggne.test.coupon.dto.CouponIssueResponse;
import org.ggne.test.coupon.dto.CouponResponse;
import org.ggne.test.coupon.repository.CouponIssueRepository;
import org.ggne.test.coupon.service.CouponService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CouponIssueRepository couponIssueRepository;

    // 쿠폰 생성
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CouponResponse> createCoupon(@RequestBody @Valid CouponCreateRequest request) {
        Long couponId = couponService.createCoupon(
                request.getName(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getTotalQuantity(),
                request.getExpiredAt()
        );
        Coupon coupon = couponService.getCoupon(couponId);
        return ApiResponse.success(HttpStatus.CREATED.value(), CouponResponse.from(coupon));
    }

    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CouponIssueResponse> issue(
            @PathVariable Long couponId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        Long couponIssueId = couponService.issue(couponId, userId);
        Coupon coupon = couponService.getCoupon(couponId);
        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId).get();
        return ApiResponse.success(HttpStatus.CREATED.value(), CouponIssueResponse.of(couponIssue, coupon));
    }

    @GetMapping("/{couponId}")
    public ApiResponse<CouponResponse> getCoupon(@PathVariable Long couponId) {
        Coupon coupon = couponService.getCoupon(couponId);
        return ApiResponse.success(HttpStatus.OK.value(), CouponResponse.from(coupon));
    }
}
