package com.bootcloud.gateway.core;

import com.bootcloud.gateway.filter.GatewayFilter;
import com.bootcloud.gateway.model.GatewayRequest;
import com.bootcloud.gateway.model.GatewayResponse;
import com.bootcloud.gateway.model.Route;

import java.util.ArrayList;
import java.util.List;

public interface Gateway {
    GatewayResponse route(GatewayRequest request);

    void addRoute(Route route);

    void removeRoute(String path);

    List<Route> getRoutes();

    void addFilter(GatewayFilter filter);

    List<GatewayFilter> getFilters();

    void start();

    void stop();
}
