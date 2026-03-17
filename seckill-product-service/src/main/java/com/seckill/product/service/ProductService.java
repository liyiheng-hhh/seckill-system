package com.seckill.product.service;

import com.seckill.product.entity.Product;
import com.seckill.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public Long addProduct(String name, String description, double price) {
        Product product = Product.builder()
                .name(name)
                .description(description)
                .price(java.math.BigDecimal.valueOf(price))
                .build();
        productMapper.insert(product);
        return product.getId();
    }

    public Product getProduct(Long productId) {
        return productMapper.selectById(productId);
    }

    public List<Product> listProducts(int page, int size) {
        int offset = (page - 1) * size;
        return productMapper.selectList(offset, size);
    }
}
