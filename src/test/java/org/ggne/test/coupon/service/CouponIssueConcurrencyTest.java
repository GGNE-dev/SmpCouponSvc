package org.ggne.test.coupon.service;

import org.ggne.test.coupon.domain.Coupon;
import org.ggne.test.coupon.repository.CouponIssueRepository;
import org.ggne.test.coupon.repository.CouponRepository;
import org.ggne.test.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CouponIssueConcurrencyTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponIssueRepository couponIssueRepository;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("선착순 100명 쿠폰에 100명이 동시에 발급 요청을 보내면, 정확히 100개만 발급되어야 한다 (실패 예상)")
    void concurrencyTest() throws InterruptedException {
        // given: 100개 한정 쿠폰 생성
        int totalQuantity = 100;
        Long couponId = couponService.createCoupon(
                "선착순 테스트 쿠폰",
                "FIXED",
                1000,
                totalQuantity,
                LocalDateTime.now().plusDays(7)
        );

        // 사용자를 미리 100명 생성 (중복 발급 체크를 피하기 위함)
        Long[] userIds = new Long[totalQuantity];
        for (int i = 0; i < totalQuantity; i++) {
            userIds[i] = userService.signup("user" + i + "@test.com", "사용자" + i);
        }

        // 동시성 테스트를 위한 설정
        int threadCount = totalQuantity;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when: 100명이 동시에 발급 시도
        for (int i = 0; i < threadCount; i++) {
            Long userId = userIds[i];
            executorService.submit(() -> {
                try {
                    couponService.issue(couponId, userId);
                } catch (Exception e) {
                    System.err.println("발급 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then: 발급된 총 수량 확인
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        long issuedCount = couponIssueRepository.count();

        System.out.println("### 최종 발급 수량: " + coupon.getIssuedQuantity());
        System.out.println("### DB에 저장된 발급 내역 수: " + issuedCount);

        // 현재 코드(A-)로는 Race Condition 때문에 100보다 클 확률이 높습니다.
        // 이 단언문(Assertion)이 실패하는 것이 이 단계의 목표입니다.
        assertThat(coupon.getIssuedQuantity()).isEqualTo(totalQuantity);
    }
}
