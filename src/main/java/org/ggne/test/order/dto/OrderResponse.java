package org.ggne.test.order.dto;

import lombok.Builder;
import lombok.Getter;
import org.ggne.test.order.domain.Orders;

import java.time.LocalDateTime;

@Getter
@Builder
public class OrderResponse {
    private Long orderId;
    private String itemName;
    private int originalPrice;
    private int discountAmount;
    private int finalPrice;
    private String status;
    private LocalDateTime orderedAt;

    public static OrderResponse from(Orders order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .itemName(order.getItemName())
                .originalPrice(order.getOriginalPrice())
                .discountAmount(order.getDiscountAmount())
                .finalPrice(order.getFinalPrice())
                .status(order.getStatus().name())
                .orderedAt(order.getOrderedAt())
                .build();
    }
}
