package com.seckill.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.order.dto.OrderInventorySettleMessage;
import com.seckill.order.dto.SeckillOrderMessage;
import com.seckill.order.entity.Order;
import com.seckill.order.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${seckill.kafka.topic.order-inventory-settle}")
    private String orderInventorySettleTopic;

    public OrderService(
            OrderMapper orderMapper,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate) {
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
    }

    public Order createOrder(Long userId, Long productId, int quantity) {
        Order order = Order.builder()
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .status("PENDING")
                .build();
        orderMapper.insert(order);
        publishInventorySettle(order);
        return order;
    }

    public Order getOrder(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    /**
     * 支付与订单状态更新：单库本地事务 + 条件更新实现幂等（重复支付不产生副作用）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(Long orderId, String paymentMethod) {
        int rows = orderMapper.updateStatusIfPending(orderId, "PAID");
        return rows > 0;
    }

    @KafkaListener(topics = "${seckill.kafka.topic.order-create}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeSeckillOrder(String payload) {
        SeckillOrderMessage message = parseSeckillMessage(payload);
        createOrderIfAbsent(message.getUserId(), message.getProductId(), message.getQuantity());
    }

    private void createOrderIfAbsent(Long userId, Long productId, Integer quantity) {
        Order existing = orderMapper.selectByUserAndProduct(userId, productId);
        if (existing != null) {
            publishInventorySettle(existing);
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
            publishInventorySettle(order);
        } catch (DuplicateKeyException e) {
            Order persisted = orderMapper.selectByUserAndProduct(userId, productId);
            if (persisted != null) {
                publishInventorySettle(persisted);
            }
        }
    }

    private void publishInventorySettle(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        OrderInventorySettleMessage msg = new OrderInventorySettleMessage(
                order.getId(),
                order.getUserId(),
                order.getProductId(),
                order.getQuantity());
        try {
            String json = objectMapper.writeValueAsString(msg);
            kafkaTemplate.send(orderInventorySettleTopic, String.valueOf(order.getId()), json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("订单库存结算消息序列化失败", e);
        }
    }

    private SeckillOrderMessage parseSeckillMessage(String payload) {
        try {
            return objectMapper.readValue(payload, SeckillOrderMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("非法秒杀消息: " + payload, e);
        }
    }
}
