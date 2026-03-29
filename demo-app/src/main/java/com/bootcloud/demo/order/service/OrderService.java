package com.bootcloud.demo.order.service;

import com.bootcloud.core.annotation.Service;
import com.bootcloud.circuitbreaker.annotation.CircuitBreaker;
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

    public OrderService() {
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
}
