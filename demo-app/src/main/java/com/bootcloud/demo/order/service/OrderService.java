package com.bootcloud.demo.order.service;

import com.bootcloud.core.annotation.Service;
import com.bootcloud.circuitbreaker.annotation.CircuitBreaker;
import com.bootcloud.circuitbreaker.core.CircuitBreaker;
import com.bootcloud.circuitbreaker.core.CircuitBreakerConfig;
import com.bootcloud.circuitbreaker.factory.CircuitBreakerFactory;
import com.bootcloud.demo.order.model.Order;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {
    private final Map<Long, Order> orderMap = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final CircuitBreaker userCircuitBreaker;

    public OrderService() {
        this.userCircuitBreaker = CircuitBreakerFactory.create(
                CircuitBreakerConfig.builder()
                        .name("user-service-cb")
                        .failureThreshold(3)
                        .resetTimeout(5000)
                        .build()
        );
    }

    @CircuitBreaker(name = "order-cb", failureThreshold = 5, resetTimeout = 10000)
    public Order createOrder(Long userId, Long productId, Integer quantity) {
        Order order = new Order(idGenerator.getAndIncrement(), userId, productId, quantity, BigDecimal.ZERO);
        orderMap.put(order.getId(), order);
        return order;
    }

    public Order getOrder(Long id) {
        return orderMap.get(id);
    }

    @CircuitBreaker(name = "order-cb", failureThreshold = 5, resetTimeout = 10000)
    public Order updateOrder(Long id, Integer quantity) {
        Order order = orderMap.get(id);
        if (order != null) {
            order.setQuantity(quantity);
            return order;
        }
        return null;
    }

    public boolean deleteOrder(Long id) {
        return orderMap.remove(id) != null;
    }

    public Order createOrderWithFallback(Long userId, Long productId, Integer quantity) {
        try {
            return userCircuitBreaker.execute(
                    () -> createOrder(userId, productId, quantity),
                    () -> createFallbackOrder(userId, productId, quantity)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create order", e);
        }
    }

    private Order createFallbackOrder(Long userId, Long productId, Integer quantity) {
        return new Order(-1L, userId, productId, quantity, BigDecimal.ZERO);
    }
}
