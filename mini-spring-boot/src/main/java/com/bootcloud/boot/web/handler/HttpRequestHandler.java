package com.bootcloud.boot.web.handler;

import com.bootcloud.boot.web.annotation.GetMapping;
import com.bootcloud.boot.web.annotation.PostMapping;
import com.bootcloud.boot.web.annotation.RequestBody;
import com.bootcloud.boot.web.annotation.RequestMapping;
import com.bootcloud.boot.web.annotation.RestController;
import com.bootcloud.core.context.ApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ApplicationContext applicationContext;
    private final Map<String, HandlerMethod> handlerMethods;

    public HttpRequestHandler(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.handlerMethods = new HashMap<>();
        scanHandlerMethods();
    }

    private void scanHandlerMethods() {
        for (String beanName : applicationContext.getBeanDefinitionMap().keySet()) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = bean.getClass();

            if (beanClass.isAnnotationPresent(RestController.class)) {
                String basePath = beanClass.getAnnotation(RequestMapping.class).value();
                scanClassMethods(bean, beanClass, basePath);
            }
        }
        logger.info("Scanned {} handler methods", handlerMethods.size());
    }

    private void scanClassMethods(Object bean, Class<?> beanClass, String basePath) {
        for (Method method : beanClass.getDeclaredMethods()) {
            String path = "";
            HttpMethod httpMethod = HttpMethod.GET;

            if (method.isAnnotationPresent(GetMapping.class)) {
                path = method.getAnnotation(GetMapping.class).value();
                httpMethod = HttpMethod.GET;
            } else if (method.isAnnotationPresent(PostMapping.class)) {
                path = method.getAnnotation(PostMapping.class).value();
                httpMethod = HttpMethod.POST;
            } else if (method.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping mapping = method.getAnnotation(RequestMapping.class);
                path = mapping.value();
                httpMethod = HttpMethod.valueOf(mapping.method());
            }

            if (!path.isEmpty()) {
                String fullPath = basePath + path;
                handlerMethods.put(fullPath, new HandlerMethod(bean, method, httpMethod));
                logger.debug("Mapped {} {} to {}.{}", httpMethod, fullPath, beanClass.getSimpleName(), method.getName());
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            String uri = request.uri();
            String path = extractPath(uri);
            HttpMethod method = request.method();

            logger.info("Received {} request to {}", method, path);

            HandlerMethod handler = findHandler(path, method);
            if (handler == null) {
                sendNotFound(ctx);
                return;
            }

            Object result = invokeHandler(handler, request);
            sendResponse(ctx, result);

        } catch (Exception e) {
            logger.error("Error processing request", e);
            sendError(ctx, e);
        }
    }

    private String extractPath(String uri) {
        int queryIndex = uri.indexOf('?');
        return queryIndex > 0 ? uri.substring(0, queryIndex) : uri;
    }

    private HandlerMethod findHandler(String path, HttpMethod method) {
        for (Map.Entry<String, HandlerMethod> entry : handlerMethods.entrySet()) {
            if (pathMatches(entry.getKey(), path) && entry.getValue().getMethod().equals(method)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean pathMatches(String pattern, String path) {
        if (pattern.equals(path)) {
            return true;
        }
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return path.startsWith(prefix);
        }
        return false;
    }

    private Object invokeHandler(HandlerMethod handler, FullHttpRequest request) throws Exception {
        Method method = handler.getMethod();
        Object[] args = resolveArguments(method, request);
        return method.invoke(handler.getBean(), args);
    }

    private Object[] resolveArguments(Method method, FullHttpRequest request) throws Exception {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            
            if (parameter.isAnnotationPresent(RequestBody.class)) {
                String content = request.content().toString(io.netty.util.CharsetUtil.UTF_8);
                args[i] = objectMapper.readValue(content, parameter.getType());
            } else {
                args[i] = null;
            }
        }

        return args;
    }

    private void sendResponse(ChannelHandlerContext ctx, Object result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.OK,
                    io.netty.buffer.Unpooled.copiedBuffer(json, io.netty.util.CharsetUtil.UTF_8)
            );

            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);

            ctx.writeAndFlush(response);
        } catch (Exception e) {
            logger.error("Error sending response", e);
            sendError(ctx, e);
        }
    }

    private void sendNotFound(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NOT_FOUND,
                io.netty.buffer.Unpooled.copiedBuffer("Not Found", io.netty.util.CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    private void sendError(ChannelHandlerContext ctx, Exception e) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.INTERNAL_SERVER_ERROR,
                io.netty.buffer.Unpooled.copiedBuffer("Internal Server Error", io.netty.util.CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response);
    }

    private static class HandlerMethod {
        private final Object bean;
        private final Method method;
        private final HttpMethod httpMethod;

        public HandlerMethod(Object bean, Method method, HttpMethod httpMethod) {
            this.bean = bean;
            this.method = method;
            this.httpMethod = httpMethod;
        }

        public Object getBean() {
            return bean;
        }

        public Method getMethod() {
            return method;
        }

        public HttpMethod getMethod() {
            return httpMethod;
        }
    }
}
