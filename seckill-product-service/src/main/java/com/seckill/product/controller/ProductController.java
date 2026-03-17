package com.seckill.product.controller;

import com.seckill.product.entity.Product;
import com.seckill.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "商品服务", description = "商品管理 API")
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "添加商品")
    @PostMapping
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public Map<String, Long> addProduct(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        double price = ((Number) body.get("price")).doubleValue();
        Long productId = productService.addProduct(name, description, price);
        return Map.of("productId", productId);
    }

    @Operation(summary = "获取商品详情")
    @GetMapping("/{productId}")
    public Product getProduct(@PathVariable Long productId) {
        return productService.getProduct(productId);
    }

    @Operation(summary = "查询商品列表")
    @GetMapping
    public List<Product> listProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return productService.listProducts(page, size);
    }
}
