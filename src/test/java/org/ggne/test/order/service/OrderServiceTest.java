package org.ggne.test.order.service;

import org.ggne.test.common.exception.BusinessException;
import org.ggne.test.common.exception.ErrorCode;
import org.ggne.test.coupon.domain.CouponIssue;
import org.ggne.test.coupon.domain.IssueStatus;
import org.ggne.test.coupon.repository.CouponIssueRepository;
import org.ggne.test.coupon.repository.CouponRepository;
import org.ggne.test.coupon.service.CouponService;
import org.ggne.test.order.domain.OrderStatus;
import org.ggne.test.order.domain.Orders;
import org.ggne.test.order.repository.OrderRepository;
import org.ggne.test.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private org.ggne.test.user.repository.UserRepository userRepository;

    private Long userId;
    private Long couponId;

    @BeforeEach
    void setUp() {
        // 데이터 초기화 (순서 주의: 외래키 제약 조건)
        orderRepository.deleteAll();
        couponIssueRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();

        userId = userService.signup("test@test.com", "테스터");
        couponId = couponService.createCoupon("테스트 쿠폰", "FIXED", 1000, 100, LocalDateTime.now().plusDays(7));
    }

    @Test
    @DisplayName("쿠폰 없이 주문을 생성할 수 있다.")
    void createOrderWithoutCoupon() {
        // when
        Long orderId = orderService.createOrder(userId, "상품A", 10000, null);

        // then
        Orders order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getFinalPrice()).isEqualTo(10000);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("쿠폰을 사용하여 주문을 생성하면 할인이 적용된다.")
    void createOrderWithCoupon() {
        // given
        Long couponIssueId = couponService.issue(couponId, userId);

        // when
        Long orderId = orderService.createOrder(userId, "상품A", 10000, couponIssueId);

        // then
        Orders order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getDiscountAmount()).isEqualTo(1000);
        assertThat(order.getFinalPrice()).isEqualTo(9000);
        
        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId).orElseThrow();
        assertThat(couponIssue.getStatus()).isEqualTo(IssueStatus.USED);
    }

    @Test
    @DisplayName("주문을 결제하면 상태가 PAID로 변경된다.")
    void payOrder() {
        // given
        Long orderId = orderService.createOrder(userId, "상품A", 10000, null);

        // when
        orderService.payOrder(orderId, userId);

        // then
        Orders order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("주문을 취소하면 상태가 CANCELLED로 변경되고 사용된 쿠폰이 복구된다.")
    void cancelOrder() {
        // given
        Long couponIssueId = couponService.issue(couponId, userId);
        Long orderId = orderService.createOrder(userId, "상품A", 10000, couponIssueId);

        // when
        orderService.cancelOrder(orderId, userId);

        // then
        Orders order = orderRepository.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId).orElseThrow();
        assertThat(couponIssue.getStatus()).isEqualTo(IssueStatus.AVAILABLE);
        assertThat(couponIssue.getUsedAt()).isNull();
    }

    @Test
    @DisplayName("본인의 주문이 아니면 결제할 수 없다.")
    void payOrderFailByOtherUser() {
        // given
        Long otherUserId = userService.signup("other@test.com", "다른사람");
        Long orderId = orderService.createOrder(userId, "상품A", 10000, null);

        // when & then
        assertThatThrownBy(() -> orderService.payOrder(orderId, otherUserId))
                .isInstanceOf(BusinessException.class);
    }
}
