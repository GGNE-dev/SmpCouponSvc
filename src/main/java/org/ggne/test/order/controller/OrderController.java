package org.ggne.test.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ggne.test.common.response.ApiResponse;
import org.ggne.test.order.domain.Orders;
import org.ggne.test.order.dto.OrderCreateRequest;
import org.ggne.test.order.dto.OrderResponse;
import org.ggne.test.order.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> createOrder(
            @RequestBody @Valid OrderCreateRequest request,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        Long orderId = orderService.createOrder(
                userId,
                request.getItemName(),
                request.getOriginalPrice(),
                request.getCouponIssueId()
        );
        Orders order = orderService.getOrder(orderId);
        return ApiResponse.success(HttpStatus.CREATED.value(), OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long orderId) {
        Orders order = orderService.getOrder(orderId);
        return ApiResponse.success(HttpStatus.OK.value(), OrderResponse.from(order));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<Void> payOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        orderService.payOrder(orderId, userId);
        return ApiResponse.success(HttpStatus.OK.value(), null);
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<Void> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        orderService.cancelOrder(orderId, userId);
        return ApiResponse.success(HttpStatus.OK.value(), null);
    }
}
