package com.seckill.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单创建成功后通知库存服务落库扣减（消息最终一致性）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInventorySettleMessage {
    private Long orderId;
    private Long userId;
    private Long productId;
    private Integer quantity;
}
