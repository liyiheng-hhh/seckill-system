package com.seckill.inventory.controller;

import com.seckill.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "库存服务", description = "库存查询与扣减 API")
@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Operation(summary = "查询库存")
    @GetMapping("/{productId}")
    public Map<String, Object> getStock(@PathVariable Long productId) {
        return inventoryService.getStock(productId);
    }

    @Operation(summary = "扣减库存")
    @PutMapping("/{productId}/deduct")
    public Map<String, Object> deductStock(@PathVariable Long productId, @RequestBody Map<String, Integer> body) {
        int quantity = body.getOrDefault("quantity", 1);
        return inventoryService.deductStock(productId, quantity);
    }
}
