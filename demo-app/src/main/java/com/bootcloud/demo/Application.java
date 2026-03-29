package com.bootcloud.demo;

import com.bootcloud.boot.web.annotation.SpringBootApplication;
import com.bootcloud.boot.SpringApplication;
import com.bootcloud.demo.goods.service.ProductService;
import com.bootcloud.demo.order.service.OrderService;
import com.bootcloud.demo.user.service.UserService;
import com.bootcloud.demo.user.model.User;

import java.math.BigDecimal;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        
        initializeData();
    }

    private static void initializeData() {
        UserService userService = SpringApplication.getApplicationContext().getBean(UserService.class);
        OrderService orderService = SpringApplication.getApplicationContext().getBean(OrderService.class);
        ProductService productService = SpringApplication.getApplicationContext().getBean(ProductService.class);

        User user1 = userService.createUser("张三", "zhangsan@example.com", 25);
        User user2 = userService.createUser("李四", "lisi@example.com", 30);

        orderService.createOrder(user1.getId(), 1L, 2);
        orderService.createOrder(user2.getId(), 2L, 1);

        productService.createProduct("iPhone 15", "Apple智能手机", new BigDecimal("7999.00"), 100);
        productService.createProduct("MacBook Pro", "Apple笔记本电脑", new BigDecimal("12999.00"), 50);

        System.out.println("=== 初始化数据完成 ===");
        System.out.println("用户: " + user1);
        System.out.println("用户: " + user2);
        System.out.println("产品1: " + productService.getProduct(1L));
        System.out.println("产品2: " + productService.getProduct(2L));
    }
}
