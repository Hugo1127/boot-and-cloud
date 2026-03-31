package com.bootcloud.demo.user.service;

import com.bootcloud.core.annotation.Autowired;
import com.bootcloud.core.annotation.Service;
import com.bootcloud.demo.user.model.User;
import com.bootcloud.demo.user.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String name, String email, Integer age) {
        User user = new User(null, name, email, age);
        return userRepository.save(user);
    }

    public User getUser(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(Long id, String name, String email, Integer age) {
        User user = userRepository.findById(id);
        if (user != null) {
            user.setName(name);
            user.setEmail(email);
            user.setAge(age);
            return userRepository.save(user);
        }
        return null;
    }

    public boolean deleteUser(Long id) {
        return userRepository.deleteById(id) != null;
    }
}
