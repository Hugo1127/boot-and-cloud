package com.bootcloud.core.test.beans;

import com.bootcloud.core.annotation.Repository;

@Repository("customUserRepo")
public class CustomNamedRepository {
    public String findById(String id) {
        return "CustomUser-" + id;
    }
}
