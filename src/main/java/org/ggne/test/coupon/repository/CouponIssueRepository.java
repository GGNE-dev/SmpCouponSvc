package org.ggne.test.coupon.repository;

import org.ggne.test.coupon.domain.CouponIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {
    boolean existsByCouponIdAndUserId(Long couponId, Long userId);
    List<CouponIssue> findAllByUserId(Long userId);
}
