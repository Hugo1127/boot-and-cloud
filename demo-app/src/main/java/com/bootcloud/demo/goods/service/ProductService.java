package com.bootcloud.demo.goods.service;

import com.bootcloud.core.annotation.Service;
import com.bootcloud.demo.goods.model.Product;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductService {
    private final Map<Long, Product> productMap = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public Product createProduct(String name, String description, BigDecimal price, Integer stock) {
        Product product = new Product(idGenerator.getAndIncrement(), name, description, price, stock);
        productMap.put(product.getId(), product);
        return product;
    }

    public Product getProduct(Long id) {
        return productMap.get(id);
    }

    public Product updateProduct(Long id, String name, String description, BigDecimal price, Integer stock) {
        Product product = productMap.get(id);
        if (product != null) {
            product.setName(name);
            product.setDescription(description);
            product.setPrice(price);
            product.setStock(stock);
            return product;
        }
        return null;
    }

    public boolean deleteProduct(Long id) {
        return productMap.remove(id) != null;
    }

    public Product decreaseStock(Long id, Integer quantity) {
        Product product = productMap.get(id);
        if (product != null && product.getStock() >= quantity) {
            product.setStock(product.getStock() - quantity);
            return product;
        }
        return null;
    }
}
