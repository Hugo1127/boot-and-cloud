package com.bootcloud.gateway.test;

import com.bootcloud.gateway.core.Gateway;
import com.bootcloud.gateway.core.impl.DefaultGateway;
import com.bootcloud.gateway.filter.impl.LoggingFilter;
import com.bootcloud.gateway.filter.impl.RateLimitFilter;
import com.bootcloud.gateway.model.GatewayRequest;
import com.bootcloud.gateway.model.GatewayResponse;
import com.bootcloud.gateway.model.Route;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GatewayTest {
    private Gateway gateway;

    @BeforeEach
    public void setUp() {
        gateway = new DefaultGateway();
        gateway.addFilter(new LoggingFilter());
        gateway.addFilter(new RateLimitFilter(100));
        
        Route route1 = new Route("/api/user", "user-service", "http://localhost:8081");
        route1.setOrder(1);
        
        Route route2 = new Route("/api/order", "order-service", "http://localhost:8082");
        route2.setOrder(2);
        
        gateway.addRoute(route1);
        gateway.addRoute(route2);
        
        gateway.start();
    }

    @AfterEach
    public void tearDown() {
        gateway.stop();
    }

    @Test
    public void testSuccessfulRoute() {
        GatewayRequest request = new GatewayRequest();
        request.setPath("/api/user/1");
        request.setMethod("GET");
        
        GatewayResponse response = gateway.route(request);
        
        assertEquals(200, response.getStatus());
        assertTrue(response.getBody().contains("Forwarded to:"));
    }

    @Test
    public void testRouteNotFound() {
        GatewayRequest request = new GatewayRequest();
        request.setPath("/api/product/1");
        request.setMethod("GET");
        
        GatewayResponse response = gateway.route(request);
        
        assertEquals(404, response.getStatus());
        assertEquals("Not Found", response.getBody());
    }

    @Test
    public void testMultipleRoutes() {
        GatewayRequest request1 = new GatewayRequest();
        request1.setPath("/api/user/1");
        request1.setMethod("GET");
        
        GatewayRequest request2 = new GatewayRequest();
        request2.setPath("/api/order/1");
        request2.setMethod("GET");
        
        GatewayResponse response1 = gateway.route(request1);
        GatewayResponse response2 = gateway.route(request2);
        
        assertEquals(200, response1.getStatus());
        assertEquals(200, response2.getStatus());
    }

    @Test
    public void testDisabledRoute() {
        Route route = new Route("/api/goods", "goods-service", "http://localhost:8083");
        route.setEnabled(false);
        gateway.addRoute(route);
        
        GatewayRequest request = new GatewayRequest();
        request.setPath("/api/goods/1");
        request.setMethod("GET");
        
        GatewayResponse response = gateway.route(request);
        
        assertEquals(404, response.getStatus());
    }

    @Test
    public void testGatewayNotRunning() {
        Gateway stoppedGateway = new DefaultGateway();
        
        GatewayRequest request = new GatewayRequest();
        request.setPath("/api/test");
        request.setMethod("GET");
        
        GatewayResponse response = stoppedGateway.route(request);
        
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testAddAndRemoveRoute() {
        Route route = new Route("/api/new", "new-service", "http://localhost:8084");
        gateway.addRoute(route);
        
        assertEquals(3, gateway.getRoutes().size());
        
        gateway.removeRoute("/api/new");
        
        assertEquals(2, gateway.getRoutes().size());
    }
}
