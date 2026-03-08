package org.ggne.test.coupon.service;

import org.ggne.test.coupon.domain.Coupon;
import org.ggne.test.coupon.repository.CouponIssueRepository;
import org.ggne.test.coupon.repository.CouponRepository;
import org.ggne.test.user.repository.UserRepository;
import org.ggne.test.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        orderRepositoryDeleteAll(); // OrderRepository가 있다면 삭제 필요하나 현재 이 파일에선 직접 참조 안함
        couponIssueRepository.deleteAll();
        couponRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void orderRepositoryDeleteAll() {
        // OrderRepository 생략 가능 (의존성 최소화를 위해)
    }

    @Test
    @DisplayName("분산 락(Redisson)을 사용한 선착순 쿠폰 발급 동시성 테스트")
    void concurrencyTestWithDistributedLock() throws InterruptedException {
        runTest("Distributed Lock", (couponId, userId) -> couponService.issue(couponId, userId));
    }

    @Test
    @DisplayName("비관적 락(Pessimistic Lock)을 사용한 선착순 쿠폰 발급 동시성 테스트")
    void concurrencyTestWithPessimisticLock() throws InterruptedException {
        runTest("Pessimistic Lock", (couponId, userId) -> couponService.issueWithPessimisticLock(couponId, userId));
    }

    private void runTest(String testName, java.util.function.BiConsumer<Long, Long> issueMethod) throws InterruptedException {
        // given: 100개 한정 쿠폰 생성
        int totalQuantity = 100;
        int requestCount = 100; // 100명이 동시에 요청
        Long couponId = couponService.createCoupon(
                testName + " 테스트 쿠폰",
                "FIXED",
                1000,
                totalQuantity,
                LocalDateTime.now().plusDays(7)
        );

        // 사용자를 미리 100명 생성 (트랜잭션 분리 문제를 위해 미리 DB에 커밋)
        Long[] userIds = new Long[requestCount];
        for (int i = 0; i < requestCount; i++) {
            userIds[i] = userService.signup("user_" + testName.replace(" ", "") + i + "@test.com", "사용자" + i);
        }

        // 동시성 테스트를 위한 설정
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(requestCount);

        long startTime = System.currentTimeMillis();

        // when: 100명이 동시에 발급 시도
        for (int i = 0; i < requestCount; i++) {
            Long userId = userIds[i];
            executorService.submit(() -> {
                try {
                    issueMethod.accept(couponId, userId);
                } catch (Exception e) {
                    // System.err.println("발급 실패: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();

        // then: 발급된 총 수량 확인
        Coupon coupon = couponRepository.findById(couponId).orElseThrow();
        long dbCount = couponIssueRepository.countByCouponId(couponId);

        System.out.println("### [" + testName + "] 결과 ###");
        System.out.println("소요 시간: " + (endTime - startTime) + "ms");
        System.out.println("최종 발급 수량(Entity): " + coupon.getIssuedQuantity());
        System.out.println("DB 저장된 발급 내역 수: " + dbCount);

        assertThat(coupon.getIssuedQuantity()).isEqualTo(totalQuantity);
        assertThat(dbCount).isEqualTo(totalQuantity);
        
        executorService.shutdown();
    }
}
