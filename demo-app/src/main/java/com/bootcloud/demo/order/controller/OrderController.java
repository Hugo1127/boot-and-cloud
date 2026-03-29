package com.bootcloud.demo.order.controller;

import com.bootcloud.boot.web.annotation.PathVariable;
import com.bootcloud.boot.web.annotation.RequestBody;
import com.bootcloud.boot.web.annotation.RestController;
import com.bootcloud.boot.web.annotation.GetMapping;
import com.bootcloud.boot.web.annotation.PostMapping;
import com.bootcloud.demo.order.model.Order;
import com.bootcloud.demo.order.service.OrderService;

import java.util.Map;

@RestController
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/api/order/{id}")
    public Order getOrder(@PathVariable("id") Long id) {
        return orderService.getOrder(id);
    }

    @PostMapping("/api/order")
    public Order createOrder(@RequestBody Map<String, Object> body) {
        Long userId = Long.parseLong(body.get("userId").toString());
        Long productId = Long.parseLong(body.get("productId").toString());
        Integer quantity = Integer.parseInt(body.get("quantity").toString());
        return orderService.createOrder(userId, productId, quantity);
    }
}
