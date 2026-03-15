package org.ggne.test.order.domain.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OrderCancelledEvent {

    private final Long orderId;
    private final Long userId;
    private final Long couponIssueId;
}
