package com.bootcloud.demo.user.repository;

import com.bootcloud.core.annotation.Repository;
import com.bootcloud.demo.user.model.User;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class UserRepository {
    private final Map<Long, User> userMap = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        userMap.put(user.getId(), user);
        return user;
    }

    public User findById(Long id) {
        return userMap.get(id);
    }

    public User deleteById(Long id) {
        return userMap.remove(id);
    }

    public Map<Long, User> findAll() {
        return new HashMap<>(userMap);
    }
}
