package com.seckill.inventory.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.inventory.dto.OrderInventorySettleMessage;
import com.seckill.inventory.service.InventoryService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class InventorySettleListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public InventorySettleListener(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${seckill.kafka.topic.order-inventory-settle}",
            groupId = "${seckill.kafka.consumer.inventory-settle-group}")
    public void onOrderInventorySettle(String payload) {
        try {
            OrderInventorySettleMessage message = objectMapper.readValue(payload, OrderInventorySettleMessage.class);
            inventoryService.settleInventoryAfterOrderCreated(message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("非法库存结算消息: " + payload, e);
        }
    }
}
