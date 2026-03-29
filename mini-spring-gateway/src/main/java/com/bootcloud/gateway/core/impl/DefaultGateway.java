package com.bootcloud.gateway.core.impl;

import com.bootcloud.gateway.core.Gateway;
import com.bootcloud.gateway.filter.GatewayFilter;
import com.bootcloud.gateway.filter.GatewayFilterChain;
import com.bootcloud.gateway.model.GatewayRequest;
import com.bootcloud.gateway.model.GatewayResponse;
import com.bootcloud.gateway.model.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultGateway implements Gateway {
    private static final Logger log = LoggerFactory.getLogger(DefaultGateway.class);
    
    private final List<Route> routes;
    private final List<GatewayFilter> filters;
    private volatile boolean running;

    public DefaultGateway() {
        this.routes = new CopyOnWriteArrayList<>();
        this.filters = new CopyOnWriteArrayList<>();
        this.running = false;
    }

    @Override
    public GatewayResponse route(GatewayRequest request) {
        if (!running) {
            log.error("Gateway is not running");
            return GatewayResponse.internalError("Gateway is not running");
        }

        log.debug("Routing request: {}", request);
        
        Route route = findRoute(request.getPath());
        if (route == null) {
            log.warn("No route found for path: {}", request.getPath());
            return GatewayResponse.notFound();
        }

        if (!route.isEnabled()) {
            log.warn("Route is disabled: {}", route);
            return GatewayResponse.notFound();
        }

        return executeFilters(request, route);
    }

    @Override
    public void addRoute(Route route) {
        log.info("Adding route: {}", route);
        routes.add(route);
        routes.sort(Comparator.comparingInt(Route::getOrder));
    }

    @Override
    public void removeRoute(String path) {
        log.info("Removing route for path: {}", path);
        routes.removeIf(route -> route.getPath().equals(path));
    }

    @Override
    public List<Route> getRoutes() {
        return new ArrayList<>(routes);
    }

    @Override
    public void addFilter(GatewayFilter filter) {
        log.info("Adding filter: {}", filter.getClass().getSimpleName());
        filters.add(filter);
    }

    @Override
    public List<GatewayFilter> getFilters() {
        return new ArrayList<>(filters);
    }

    @Override
    public void start() {
        log.info("Starting Gateway");
        this.running = true;
    }

    @Override
    public void stop() {
        log.info("Stopping Gateway");
        this.running = false;
    }

    private Route findRoute(String path) {
        for (Route route : routes) {
            if (route.matches(path)) {
                return route;
            }
        }
        return null;
    }

    private GatewayResponse executeFilters(GatewayRequest request, Route route) {
        GatewayFilterChain chain = new GatewayFilterChain() {
            private int currentPosition = 0;

            @Override
            public GatewayResponse doFilter(GatewayRequest req) {
                if (currentPosition < filters.size()) {
                    GatewayFilter filter = filters.get(currentPosition++);
                    return filter.filter(req, this);
                }
                return forwardToBackend(req, route);
            }
        };
        
        return chain.doFilter(request);
    }

    private GatewayResponse forwardToBackend(GatewayRequest request, Route route) {
        log.info("Forwarding request to backend: {} -> {}", request.getPath(), route.getUrl());
        
        try {
            return doForward(request, route);
        } catch (Exception e) {
            log.error("Error forwarding request", e);
            return GatewayResponse.internalError(e.getMessage());
        }
    }

    private GatewayResponse doForward(GatewayRequest request, Route route) throws Exception {
        String targetUrl = route.getUrl() + request.getPath();
        log.debug("Target URL: {}", targetUrl);
        
        return GatewayResponse.ok("Forwarded to: " + targetUrl);
    }
}
