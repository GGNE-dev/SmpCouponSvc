package org.ggne.test.coupon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_coupon_user", columnNames = {"coupon_id", "user_id"})
    }
)
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IssueStatus status;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    private LocalDateTime usedAt;

    public CouponIssue(Long couponId, Long userId) {
        this.couponId = couponId;
        this.userId = userId;
        this.status = IssueStatus.AVAILABLE;
    }

    public void use() {
        this.status = IssueStatus.USED;
        this.usedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = IssueStatus.AVAILABLE;
        this.usedAt = null;
    }
}
