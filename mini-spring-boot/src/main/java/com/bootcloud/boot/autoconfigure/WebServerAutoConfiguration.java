package com.bootcloud.boot.autoconfigure;

import com.bootcloud.boot.annotation.ConditionalOnProperty;
import com.bootcloud.boot.context.BootApplicationContext;
import com.bootcloud.boot.web.server.NettyWebServer;
import com.bootcloud.core.context.ApplicationContext;

public class WebServerAutoConfiguration {

    @ConditionalOnProperty(name = "web.server.type", value = "netty", matchIfMissing = true)
    public NettyWebServer nettyWebServer(BootApplicationContext context) {
        String port = context.getEnvironment().getProperty("server.port", "8080");
        String host = context.getEnvironment().getProperty("server.host", "0.0.0.0");
        return new NettyWebServer(Integer.parseInt(port), host, context);
    }
}
