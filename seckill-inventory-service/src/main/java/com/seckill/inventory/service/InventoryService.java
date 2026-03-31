package com.seckill.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.inventory.entity.Inventory;
import com.seckill.inventory.dto.SeckillOrderMessage;
import com.seckill.inventory.mapper.InventoryMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class InventoryService {

    private static final String STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String ORDER_USER_KEY_PREFIX = "seckill:order:user:";

    private final InventoryMapper inventoryMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${seckill.kafka.topic.order-create}")
    private String orderCreateTopic;

    public InventoryService(
            InventoryMapper inventoryMapper,
            StringRedisTemplate stringRedisTemplate,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.inventoryMapper = inventoryMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> getStock(Long productId) {
        Inventory inv = inventoryMapper.selectByProductId(productId);
        int stock = inv != null ? inv.getStock() : 0;
        return Map.of("productId", productId, "stock", stock);
    }

    public Map<String, Object> deductStock(Long productId, int quantity) {
        int rows = inventoryMapper.deductStock(productId, quantity);
        boolean success = rows > 0;
        Inventory inv = inventoryMapper.selectByProductId(productId);
        int remaining = inv != null ? inv.getStock() : 0;
        return Map.of("success", success, "remainingStock", remaining);
    }

    public Map<String, Object> seckill(Long userId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity 必须大于 0");
        }
        if (quantity > 1) {
            throw new IllegalArgumentException("秒杀场景当前仅支持 quantity=1");
        }

        String userOrderKey = ORDER_USER_KEY_PREFIX + userId + ":" + productId;
        Boolean firstOrder = stringRedisTemplate.opsForValue()
                .setIfAbsent(userOrderKey, "1", Duration.ofHours(24));
        if (!Boolean.TRUE.equals(firstOrder)) {
            return Map.of("success", false, "message", "请勿重复下单");
        }

        String stockKey = STOCK_KEY_PREFIX + productId;
        ensureStockLoaded(productId, stockKey);

        Long remain = stringRedisTemplate.opsForValue().increment(stockKey, -quantity);
        if (remain == null || remain < 0) {
            stringRedisTemplate.opsForValue().increment(stockKey, quantity);
            stringRedisTemplate.delete(userOrderKey);
            return Map.of("success", false, "message", "库存不足");
        }

        SeckillOrderMessage message = new SeckillOrderMessage(userId, productId, quantity);
        try {
            kafkaTemplate.send(orderCreateTopic, String.valueOf(productId), objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException ex) {
            rollbackReservation(stockKey, quantity, userOrderKey);
            throw new IllegalStateException("秒杀下单消息发送失败", ex);
        } catch (Exception ex) {
            rollbackReservation(stockKey, quantity, userOrderKey);
            throw new IllegalStateException("秒杀下单消息发送失败", ex);
        }

        return Map.of("success", true, "message", "抢购成功，订单创建中");
    }

    private void ensureStockLoaded(Long productId, String stockKey) {
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            return;
        }
        String initLockKey = "lock:init:stock:" + productId;
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(initLockKey, "1", 5, TimeUnit.SECONDS);
        if (!Boolean.TRUE.equals(locked)) {
            return;
        }
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
                return;
            }
            Inventory dbInventory = inventoryMapper.selectByProductId(productId);
            int stock = dbInventory == null ? 0 : dbInventory.getStock();
            stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(stock));
        } finally {
            stringRedisTemplate.delete(initLockKey);
        }
    }

    private void rollbackReservation(String stockKey, int quantity, String userOrderKey) {
        stringRedisTemplate.opsForValue().increment(stockKey, quantity);
        stringRedisTemplate.delete(userOrderKey);
    }
}
