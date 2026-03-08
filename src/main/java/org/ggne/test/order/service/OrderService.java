package org.ggne.test.order.service;

import lombok.RequiredArgsConstructor;
import org.ggne.test.common.exception.BusinessException;
import org.ggne.test.common.exception.ErrorCode;
import org.ggne.test.coupon.domain.Coupon;
import org.ggne.test.coupon.domain.CouponIssue;
import org.ggne.test.coupon.domain.IssueStatus;
import org.ggne.test.coupon.repository.CouponIssueRepository;
import org.ggne.test.coupon.repository.CouponRepository;
import org.ggne.test.order.domain.Orders;
import org.ggne.test.order.repository.OrderRepository;
import org.ggne.test.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final UserService userService;

    @Transactional
    public Long createOrder(Long userId, String itemName, int originalPrice, Long couponIssueId) {
        // 1. 사용자 존재 확인
        userService.getUser(userId);

        int discountAmount = 0;

        // 2. 쿠폰 적용 시 검증 및 할인 계산
        if (couponIssueId != null) {
            CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_ISSUE_NOT_FOUND));

            // 소유자 검증
            if (!couponIssue.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.COUPON_NOT_OWNED);
            }

            // 상태 검증 (AVAILABLE 여부)
            if (couponIssue.getStatus() != IssueStatus.AVAILABLE) {
                throw new BusinessException(ErrorCode.COUPON_ALREADY_USED);
            }

            // 할인 금액 계산 (Coupon 엔티티 조회 필요)
            Coupon coupon = couponRepository.findById(couponIssue.getCouponId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

            discountAmount = calculateDiscount(originalPrice, coupon);
            
            // 쿠폰 사용 처리 (USED)
            couponIssue.use();
        }

        // 3. 주문 엔티티 생성 및 저장
        int finalPrice = Math.max(0, originalPrice - discountAmount);
        Orders order = Orders.builder()
                .userId(userId)
                .couponIssueId(couponIssueId)
                .itemName(itemName)
                .originalPrice(originalPrice)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .build();

        return orderRepository.save(order).getId();
    }

    private int calculateDiscount(int originalPrice, Coupon coupon) {
        return switch (coupon.getDiscountType()) {
            case FIXED -> coupon.getDiscountValue();
            case RATE -> (int) (originalPrice * (coupon.getDiscountValue() / 100.0));
        };
    }

    public Orders getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional
    public void payOrder(Long orderId, Long userId) {
        Orders order = getOrder(orderId);

        // 소유자 검증
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_OWNED); // 주문용 권한 에러 코드가 없으므로 일단 쿠폰용 에러 사용
        }

        order.pay();
    }

    @Transactional
    public void cancelOrder(Long orderId, Long userId) {
        Orders order = getOrder(orderId);

        // 소유자 검증
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_OWNED);
        }

        // 주문 상태 변경
        order.cancel();

        // 쿠폰 복구
        if (order.getCouponIssueId() != null) {
            CouponIssue couponIssue = couponIssueRepository.findById(order.getCouponIssueId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_ISSUE_NOT_FOUND));
            couponIssue.cancel();
        }
    }
}
