package com.bootcloud.demo.user.controller;

import com.bootcloud.boot.web.annotation.PathVariable;
import com.bootcloud.boot.web.annotation.RequestBody;
import com.bootcloud.boot.web.annotation.RestController;
import com.bootcloud.boot.web.annotation.GetMapping;
import com.bootcloud.boot.web.annotation.PostMapping;
import com.bootcloud.demo.user.model.User;
import com.bootcloud.demo.user.service.UserService;

import java.util.Map;

@RestController
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/user/{id}")
    public User getUser(@PathVariable("id") Long id) {
        return userService.getUser(id);
    }

    @PostMapping("/api/user")
    public User createUser(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String email = (String) body.get("email");
        Integer age = body.get("age") != null ? Integer.parseInt(body.get("age").toString()) : null;
        return userService.createUser(name, email, age);
    }
}
