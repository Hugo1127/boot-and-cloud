package com.bootcloud.boot.condition;

import com.bootcloud.boot.context.BootApplicationContext;

public interface Condition {
    boolean matches(BootApplicationContext context);
}
