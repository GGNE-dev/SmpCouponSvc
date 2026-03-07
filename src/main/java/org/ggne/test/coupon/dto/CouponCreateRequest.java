package org.ggne.test.coupon.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponCreateRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String discountType; // FIXED, RATE

    @Min(1)
    private int discountValue;

    @Min(1)
    private int totalQuantity;

    @NotNull
    @Future
    private LocalDateTime expiredAt;

    public CouponCreateRequest(String name, String discountType, int discountValue, int totalQuantity, LocalDateTime expiredAt) {
        this.name = name;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.totalQuantity = totalQuantity;
        this.expiredAt = expiredAt;
    }
}
