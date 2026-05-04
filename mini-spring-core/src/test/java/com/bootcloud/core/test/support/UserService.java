package com.bootcloud.core.test.support;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.PostConstruct;
import com.bootcloud.core.annotation.PreDestroy;
import com.bootcloud.core.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderService orderService;

    public String getUser(String id) {
        return userRepository.findById(id);
    }

    public String getOrder(String id) {
        return orderService.findOrder(id);
    }

    @PostConstruct
    public void init() {
        logger.info("UserService initialized");
    }

    @PreDestroy
    public void destroy() {
        logger.info("UserService destroyed");
    }
}
