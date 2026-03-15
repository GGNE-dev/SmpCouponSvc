package org.ggne.test.order.service;

import lombok.RequiredArgsConstructor;
import org.ggne.test.common.domain.Money;
import org.ggne.test.common.exception.BusinessException;
import org.ggne.test.common.exception.ErrorCode;
import org.ggne.test.coupon.service.CouponService;
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
    private final UserService userService;
    private final CouponService couponService;

    @Transactional
    public Long createOrder(Long userId, String itemName, int originalPrice, Long couponIssueId) {
        // 1. 사용자 존재 확인
        userService.getUser(userId);

        // 기존 int 타입에서 Money 타입으로 변경
        Money discountAmount = new Money(0);

        // 2. 쿠폰 적용 시 검증 및 할인 계산
        if (couponIssueId != null) {
            discountAmount = couponService.validateAndUseCoupon(couponIssueId, userId, originalPrice);
        }

        // 3. 주문 엔티티 생성 및 저장
        Orders order = Orders.builder()
                .userId(userId)
                .couponIssueId(couponIssueId)
                .itemName(itemName)
                .originalPrice(originalPrice)
                .discountAmount(discountAmount.getAmount())
                .finalPrice(Math.max(0, originalPrice - discountAmount.getAmount()))
                .build();

        return orderRepository.save(order).getId();
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

        // 주문 상태 변경 및 이벤트 등록
        order.cancel();

        // 이벤트 발행을 위해 명시적으로 save() 호출
        orderRepository.save(order);
    }
}
