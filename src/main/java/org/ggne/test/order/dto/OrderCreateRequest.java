package org.ggne.test.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderCreateRequest {
    @NotBlank
    private String itemName;

    @Min(0)
    private int originalPrice;

    private Long couponIssueId; // null 가능

    public OrderCreateRequest(String itemName, int originalPrice, Long couponIssueId) {
        this.itemName = itemName;
        this.originalPrice = originalPrice;
        this.couponIssueId = couponIssueId;
    }
}
