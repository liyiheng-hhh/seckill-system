package com.seckill.order.controller;

import com.seckill.order.entity.Order;
import com.seckill.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "订单服务", description = "订单管理 API")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "创建订单")
    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        Long productId = ((Number) body.get("productId")).longValue();
        int quantity = ((Number) body.get("quantity")).intValue();
        Order order = orderService.createOrder(userId, productId, quantity);
        return Map.of("orderId", order.getId(), "status", order.getStatus());
    }

    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderId}")
    public Order getOrder(@PathVariable Long orderId) {
        return orderService.getOrder(orderId);
    }

    @Operation(summary = "支付订单")
    @PutMapping("/{orderId}/pay")
    public ResponseEntity<Map<String, String>> payOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> body) {
        String paymentMethod = body.getOrDefault("paymentMethod", "default");
        boolean paid = orderService.payOrder(orderId, paymentMethod);
        if (!paid) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "订单不存在或已支付，幂等拒绝重复支付"));
        }
        return ResponseEntity.ok(Map.of("status", "PAID"));
    }
}
