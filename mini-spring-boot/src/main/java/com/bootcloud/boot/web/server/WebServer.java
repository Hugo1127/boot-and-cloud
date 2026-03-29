package com.bootcloud.boot.web.server;

public interface WebServer {
    void start();

    void stop();

    int getPort();
}
