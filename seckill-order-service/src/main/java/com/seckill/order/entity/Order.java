package com.seckill.order.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private String status;
    private LocalDateTime createdAt;
}
