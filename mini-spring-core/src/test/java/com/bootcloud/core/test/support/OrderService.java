package com.bootcloud.core.test.support;

import com.bootcloud.core.annotation.Service;

@Service
public class OrderService {
    public String findOrder(String id) {
        return "Order-" + id;
    }
}
