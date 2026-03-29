package com.bootcloud.demo.goods.controller;

import com.bootcloud.boot.web.annotation.PathVariable;
import com.bootcloud.boot.web.annotation.RequestBody;
import com.bootcloud.boot.web.annotation.RestController;
import com.bootcloud.boot.web.annotation.GetMapping;
import com.bootcloud.boot.web.annotation.PostMapping;
import com.bootcloud.demo.goods.model.Product;
import com.bootcloud.demo.goods.service.ProductService;

import java.math.BigDecimal;
import java.util.Map;

@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/product/{id}")
    public Product getProduct(@PathVariable("id") Long id) {
        return productService.getProduct(id);
    }

    @PostMapping("/api/product")
    public Product createProduct(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        BigDecimal price = new BigDecimal(body.get("price").toString());
        Integer stock = body.get("stock") != null ? Integer.parseInt(body.get("stock").toString()) : 0;
        return productService.createProduct(name, description, price, stock);
    }
}
