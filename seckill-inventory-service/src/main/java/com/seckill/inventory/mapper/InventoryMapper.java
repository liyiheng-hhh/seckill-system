package com.seckill.inventory.mapper;

import com.seckill.inventory.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventoryMapper {

    Inventory selectByProductId(@Param("productId") Long productId);

    int insert(Inventory inventory);

    int deductStock(@Param("productId") Long productId, @Param("quantity") int quantity);
}
