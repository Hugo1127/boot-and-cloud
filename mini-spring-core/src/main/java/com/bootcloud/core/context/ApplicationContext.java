package com.bootcloud.core.context;

import com.bootcloud.core.factory.BeanFactory;
import com.bootcloud.core.event.ApplicationEvent;
import com.bootcloud.core.event.ApplicationListener;

public interface ApplicationContext extends BeanFactory {
    void refresh();

    String getId();

    String getApplicationName();

    void publishEvent(ApplicationEvent event);

    void addApplicationListener(ApplicationListener<?> listener);

    void registerShutdownHook();
}
