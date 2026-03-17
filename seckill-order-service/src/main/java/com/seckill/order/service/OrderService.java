package com.seckill.order.service;

import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrderService {

    private final OrderMapper orderMapper;

    public OrderService(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    public Order createOrder(Long userId, Long productId, int quantity) {
        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .status("PENDING")
                .build();
        orderMapper.insert(order);
        return order;
    }

    public Order getOrder(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    public void payOrder(Long orderId, String paymentMethod) {
        orderMapper.updateStatus(orderId, "PAID");
    }
}
