package org.ggne.test.coupon.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ggne.test.common.aop.CallByDistributedLockTransaction;
import org.ggne.test.common.aop.DistributedLock;
import org.ggne.test.common.domain.Money;
import org.ggne.test.common.exception.BusinessException;
import org.ggne.test.common.exception.ErrorCode;
import org.ggne.test.coupon.domain.Coupon;
import org.ggne.test.coupon.domain.CouponIssue;
import org.ggne.test.coupon.domain.DiscountType;
import org.ggne.test.coupon.domain.IssueStatus;
import org.ggne.test.coupon.dto.CouponIssueResponse;
import org.ggne.test.coupon.repository.CouponIssueRepository;
import org.ggne.test.coupon.repository.CouponRepository;
import org.ggne.test.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final UserService userService;

    private final CallByDistributedLockTransaction callByDistributedLockTransaction;

    @Transactional
    public Long createCoupon(String name, String discountType, int discountValue, int totalQuantity, LocalDateTime expiredAt) {
        Coupon coupon = Coupon.builder()
                .name(name)
                .discountType(DiscountType.valueOf(discountType))
                .discountValue(discountValue)
                .totalQuantity(totalQuantity)
                .expiredAt(expiredAt)
                .build();
        return couponRepository.save(coupon).getId();
    }

    // 쿠폰 지급 프로세스
    @DistributedLock(key = "'coupon_lock_' + #couponId")
    public Long issue(Long couponId, Long userId) {
        try {
            return (Long) callByDistributedLockTransaction.proceed(() -> {
                // 1. 사용자 존재 확인
                userService.getUser(userId);

                // 2. 쿠폰 조회
                Coupon coupon = couponRepository.findById(couponId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

                // 3. 발급 가능 여부 확인 (만료일, 총 수량)
                if (!coupon.isIssueable()) {
                    if (coupon.getExpiredAt().isBefore(LocalDateTime.now())) {
                        throw new BusinessException(ErrorCode.COUPON_EXPIRED);
                    }
                    throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
                }

                // 4. 중복 발급 확인
                if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
                    throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
                }

                // 5. 발급 내역 저장
                CouponIssue couponIssue = new CouponIssue(couponId, userId);
                couponIssueRepository.save(couponIssue);

                // 6. 쿠폰 발급 수량 증가
                coupon.incrementIssuedQuantity();

                return couponIssue.getId();
            });
        } catch (Throwable e) {
            if (e instanceof BusinessException) throw (BusinessException) e;
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public Long issueWithPessimisticLock(Long couponId, Long userId) {
        // 1. 사용자 존재 확인
        userService.getUser(userId);

        // 2. 쿠폰 조회 (비관적 락 적용)
        Coupon coupon = couponRepository.findByIdWithLock(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        // 3. 발급 가능 여부 확인
        if (!coupon.isIssueable()) {
            if (coupon.getExpiredAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException(ErrorCode.COUPON_EXPIRED);
            }
            throw new BusinessException(ErrorCode.COUPON_OUT_OF_STOCK);
        }

        // 4. 중복 발급 확인
        if (couponIssueRepository.existsByCouponIdAndUserId(couponId, userId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED);
        }

        // 5. 발급 내역 저장
        CouponIssue couponIssue = new CouponIssue(couponId, userId);
        couponIssueRepository.save(couponIssue);

        // 6. 쿠폰 발급 수량 증가
        coupon.incrementIssuedQuantity();

        return couponIssue.getId();
    }

    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
    }

    public CouponIssueResponse issueAndGetResponse(Long couponId, Long userId) {
        // 쿠폰 발급
        Long couponIssueId = issue(couponId, userId);

        // CouponIssue와 Coupon 정보를 함께 조회
        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_ISSUE_NOT_FOUND));
        Coupon coupon = couponRepository.findById(couponIssue.getCouponId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));

        return CouponIssueResponse.of(couponIssue, coupon);
    }

    // 쿠폰 복구
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void restoreCoupon(Long couponIssueId) {
        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_ISSUE_NOT_FOUND));

        couponIssue.cancel();
        couponIssueRepository.save(couponIssue);  // ← 명시적으로 save() 호출
    }

    /**
     * 쿠폰 검증 및 사용 처리
     *
     * @return 할인 금액 (Money)
     */
    @Transactional
    public Money validateAndUseCoupon(Long couponIssueId, Long userId, int originalPrice) {
        // 1. 쿠폰 발급 내역 조회
        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_ISSUE_NOT_FOUND));

        // 2. 소유자 검증
        if (!couponIssue.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_OWNED);
        }

        // 3. 상태 검증 (AVAILABLE 여부)
        if (couponIssue.getStatus() != IssueStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_USED);
        }

        // 4. 할인 금액 계산
        Coupon coupon = couponRepository.findById(couponIssue.getCouponId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND));
        Money discountAmount = coupon.calculateDiscount(new Money(originalPrice));

        // 5. 쿠폰 사용 처리
        couponIssue.use();

        return discountAmount;
    }
}