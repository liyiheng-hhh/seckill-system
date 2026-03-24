package com.seckill.product.service;

import com.seckill.product.cache.CacheClient;
import com.seckill.product.cache.CacheConstants;
import com.seckill.product.datasource.ReadOnlyDataSource;
import com.seckill.product.entity.Product;
import com.seckill.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final CacheClient cacheClient;

    public ProductService(ProductMapper productMapper, CacheClient cacheClient) {
        this.productMapper = productMapper;
        this.cacheClient = cacheClient;
    }

    public Long addProduct(String name, String description, double price) {
        Product product = Product.builder()
                .name(name)
                .description(description)
                .price(java.math.BigDecimal.valueOf(price))
                .build();
        productMapper.insert(product);
        cacheClient.delete(CacheConstants.PRODUCT_CACHE_KEY_PREFIX + product.getId());
        return product.getId();
    }

    @ReadOnlyDataSource
    public Product getProduct(Long productId) {
        return cacheClient.queryWithPassThroughAndMutex(
                CacheConstants.PRODUCT_CACHE_KEY_PREFIX,
                CacheConstants.PRODUCT_LOCK_KEY_PREFIX,
                productId,
                Product.class,
                productMapper::selectById
        );
    }

    @ReadOnlyDataSource
    public List<Product> listProducts(int page, int size) {
        int offset = (page - 1) * size;
        return productMapper.selectList(offset, size);
    }
}
