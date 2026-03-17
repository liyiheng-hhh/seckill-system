package com.seckill.product.mapper;

import com.seckill.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    Product selectById(@Param("id") Long id);

    List<Product> selectList(@Param("offset") int offset, @Param("limit") int limit);

    int insert(Product product);
}
