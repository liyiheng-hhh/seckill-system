package com.seckill.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.order.dto.SeckillOrderMessage;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    public OrderService(OrderMapper orderMapper, ObjectMapper objectMapper) {
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
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

    @KafkaListener(topics = "${seckill.kafka.topic.order-create}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeSeckillOrder(String payload) {
        SeckillOrderMessage message = parseMessage(payload);
        createOrderIfAbsent(message.getUserId(), message.getProductId(), message.getQuantity());
    }

    private void createOrderIfAbsent(Long userId, Long productId, Integer quantity) {
        Order existing = orderMapper.selectByUserAndProduct(userId, productId);
        if (existing != null) {
            return;
        }
        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity == null ? 1 : quantity)
                .status("PENDING")
                .build();
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException ignore) {
            // DB 唯一键兜底，保证消费重试不会插入重复订单
        }
    }

    private SeckillOrderMessage parseMessage(String payload) {
        try {
            return objectMapper.readValue(payload, SeckillOrderMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("非法秒杀消息: " + payload, e);
        }
    }
}
