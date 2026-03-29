package com.bootcloud.registry.server;

import com.bootcloud.registry.core.ServiceRegistry;
import com.bootcloud.registry.core.impl.InMemoryServiceRegistry;
import com.bootcloud.registry.model.ServiceInstance;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistryServer {
    private static final Logger log = LoggerFactory.getLogger(RegistryServer.class);

    private final int port;
    private final ServiceRegistry serviceRegistry;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    public RegistryServer(int port) {
        this.port = port;
        this.serviceRegistry = new InMemoryServiceRegistry(90);
    }

    public void start() throws Exception {
        log.info("Starting RegistryServer on port {}", port);
        
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new HttpServerCodec());
                            p.addLast(new HttpObjectAggregator(65536));
                            p.addLast(new RegistryHandler(serviceRegistry));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            ChannelFuture f = b.bind(port).sync();
            channel = f.channel();
            
            log.info("RegistryServer started successfully on port {}", port);
            
            channel.closeFuture().sync();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        log.info("Shutting down RegistryServer");
        serviceRegistry.shutdown();
        if (channel != null) {
            channel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }

    private static class RegistryHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private static final Logger log = LoggerFactory.getLogger(RegistryHandler.class);
        
        private final ServiceRegistry serviceRegistry;

        public RegistryHandler(ServiceRegistry serviceRegistry) {
            this.serviceRegistry = serviceRegistry;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            String uri = request.uri();
            String method = request.method().name();
            
            log.info("Received request: {} {}", method, uri);
            
            try {
                FullHttpResponse response = handleRequest(request);
                ctx.writeAndFlush(response);
            } catch (Exception e) {
                log.error("Error handling request", e);
                FullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, 
                        HttpResponseStatus.INTERNAL_SERVER_ERROR
                );
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
                ctx.writeAndFlush(response);
            }
        }

        private FullHttpResponse handleRequest(FullHttpRequest request) {
            String uri = request.uri();
            String method = request.method().name();
            
            if (uri.startsWith("/register") && "POST".equals(method)) {
                return handleRegister(request);
            } else if (uri.startsWith("/deregister") && "POST".equals(method)) {
                return handleDeregister(request);
            } else if (uri.startsWith("/renew") && "POST".equals(method)) {
                return handleRenew(request);
            } else if (uri.startsWith("/instances/") && "GET".equals(method)) {
                return handleGetInstances(uri);
            } else if (uri.startsWith("/services") && "GET".equals(method)) {
                return handleGetServices();
            } else if (uri.startsWith("/health") && "GET".equals(method)) {
                return handleHealthCheck();
            }
            
            return createResponse(HttpResponseStatus.NOT_FOUND, "Not Found");
        }

        private FullHttpResponse handleRegister(FullHttpRequest request) {
            try {
                String content = request.content().toString(io.netty.util.CharsetUtil.UTF_8);
                ServiceInstance instance = parseInstance(content);
                
                serviceRegistry.register(instance);
                
                return createResponse(HttpResponseStatus.OK, "Registered: " + instance.getInstanceId());
            } catch (Exception e) {
                log.error("Error registering instance", e);
                return createResponse(HttpResponseStatus.BAD_REQUEST, "Error: " + e.getMessage());
            }
        }

        private FullHttpResponse handleDeregister(FullHttpRequest request) {
            try {
                String content = request.content().toString(io.netty.util.CharsetUtil.UTF_8);
                ServiceInstance instance = parseInstance(content);
                
                serviceRegistry.deregister(instance);
                
                return createResponse(HttpResponseStatus.OK, "Deregistered: " + instance.getInstanceId());
            } catch (Exception e) {
                log.error("Error deregistering instance", e);
                return createResponse(HttpResponseStatus.BAD_REQUEST, "Error: " + e.getMessage());
            }
        }

        private FullHttpResponse handleRenew(FullHttpRequest request) {
            try {
                String content = request.content().toString(io.netty.util.CharsetUtil.UTF_8);
                ServiceInstance instance = parseInstance(content);
                
                serviceRegistry.renew(instance);
                
                return createResponse(HttpResponseStatus.OK, "Renewed: " + instance.getInstanceId());
            } catch (Exception e) {
                log.error("Error renewing instance", e);
                return createResponse(HttpResponseStatus.BAD_REQUEST, "Error: " + e.getMessage());
            }
        }

        private FullHttpResponse handleGetInstances(String uri) {
            try {
                String serviceId = uri.substring("/instances/".length());
                var instances = serviceRegistry.getInstances(serviceId);
                
                return createResponse(HttpResponseStatus.OK, instances.toString());
            } catch (Exception e) {
                log.error("Error getting instances", e);
                return createResponse(HttpResponseStatus.BAD_REQUEST, "Error: " + e.getMessage());
            }
        }

        private FullHttpResponse handleGetServices() {
            try {
                var services = serviceRegistry.getServices();
                return createResponse(HttpResponseStatus.OK, services.toString());
            } catch (Exception e) {
                log.error("Error getting services", e);
                return createResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
            }
        }

        private FullHttpResponse handleHealthCheck() {
            return createResponse(HttpResponseStatus.OK, "Registry Server is healthy");
        }

        private ServiceInstance parseInstance(String json) {
            String[] parts = json.split(",");
            ServiceInstance instance = new ServiceInstance();
            for (String part : parts) {
                String[] kv = part.split("=");
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim().replace("\"", "");
                    
                    switch (key) {
                        case "serviceId":
                            instance.setServiceId(value);
                            break;
                        case "instanceId":
                            instance.setInstanceId(value);
                            break;
                        case "host":
                            instance.setHost(value);
                            break;
                        case "port":
                            instance.setPort(Integer.parseInt(value));
                            break;
                    }
                }
            }
            return instance;
        }

        private FullHttpResponse createResponse(HttpResponseStatus status, String content) {
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, 
                    status,
                    io.netty.buffer.Unpooled.copiedBuffer(content, io.netty.util.CharsetUtil.UTF_8)
            );
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            return response;
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("Exception in handler", cause);
            ctx.close();
        }
    }

    public static void main(String[] args) throws Exception {
        RegistryServer server = new RegistryServer(8081);
        server.start();
    }
}
