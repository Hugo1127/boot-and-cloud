package com.bootcloud.core.event;

public interface ApplicationListener<E extends ApplicationEvent> {
    void onApplicationEvent(E event);
}
