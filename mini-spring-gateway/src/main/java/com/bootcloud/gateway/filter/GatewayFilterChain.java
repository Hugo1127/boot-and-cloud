package com.bootcloud.gateway.filter;

import com.bootcloud.gateway.model.GatewayRequest;
import com.bootcloud.gateway.model.GatewayResponse;

public interface GatewayFilterChain {
    GatewayResponse doFilter(GatewayRequest request);
}
