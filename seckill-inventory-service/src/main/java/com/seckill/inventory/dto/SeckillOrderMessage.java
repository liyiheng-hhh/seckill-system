package com.seckill.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderMessage {
    private Long userId;
    private Long productId;
    private Integer quantity;
}
