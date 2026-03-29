package com.bootcloud.core.test;

import com.bootcloud.core.annotation.Repository;

@Repository
public class UserRepository {
    public String findById(String id) {
        return "User-" + id;
    }
}
