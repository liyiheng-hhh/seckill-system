package com.seckill.inventory.service;

import com.seckill.inventory.entity.Inventory;
import com.seckill.inventory.mapper.InventoryMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;

    public InventoryService(InventoryMapper inventoryMapper) {
        this.inventoryMapper = inventoryMapper;
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
}
