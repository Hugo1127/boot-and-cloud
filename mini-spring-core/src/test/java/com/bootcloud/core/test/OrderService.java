package com.bootcloud.core.test;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.PostConstruct;
import com.bootcloud.core.annotation.PreDestroy;
import com.bootcloud.core.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private UserService userService;

    public String findOrder(String id) {
        return "Order-" + id + " for " + userService.getUser("123");
    }

    @PostConstruct
    public void init() {
        logger.info("OrderService initialized");
    }

    @PreDestroy
    public void destroy() {
        logger.info("OrderService destroyed");
    }
}
