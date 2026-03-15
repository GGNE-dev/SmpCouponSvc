package org.ggne.test.coupon.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ggne.test.coupon.service.CouponService;
import org.ggne.test.order.domain.event.OrderCancelledEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponEventHandler {

    private final CouponService couponService;

    // 트랜잭션 커밋 후 실행
    @TransactionalEventListener
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("주문 취소 이벤트 수신: orderId={}, couponIssueId={}",
                event.getOrderId(), event.getCouponIssueId());

        if (event.getCouponIssueId() != null) {
            couponService.restoreCoupon(event.getCouponIssueId());
            log.info("쿠폰 복구 완료: couponIssueId={}", event.getCouponIssueId());
        }
    }
}
