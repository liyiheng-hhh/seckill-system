package com.seckill.order.mapper;

import com.seckill.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper {

    Order selectById(@Param("id") Long id);
    
    Order selectByUserAndProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    int insert(Order order);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 仅当当前状态为 PENDING 时更新（支付幂等）
     */
    int updateStatusIfPending(@Param("id") Long id, @Param("status") String status);
}
