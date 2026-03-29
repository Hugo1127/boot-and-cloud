package com.bootcloud.boot.web.server;

import com.bootcloud.boot.web.annotation.RequestMapping;
import com.bootcloud.boot.web.handler.HttpRequestHandler;
import com.bootcloud.core.context.ApplicationContext;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyWebServer implements WebServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyWebServer.class);

    private final int port;
    private final String host;
    private final ApplicationContext applicationContext;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private HttpRequestHandler requestHandler;

    public NettyWebServer(int port, String host, ApplicationContext applicationContext) {
        this.port = port;
        this.host = host;
        this.applicationContext = applicationContext;
        this.requestHandler = new HttpRequestHandler(applicationContext);
    }

    @Override
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new HttpRequestDecoder());
                            pipeline.addLast(new HttpObjectAggregator(65536));
                            pipeline.addLast(new HttpResponseEncoder());
                            pipeline.addLast(new ChunkedWriteHandler());
                            pipeline.addLast(requestHandler);
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(host, port).sync();
            serverChannel = future.channel();
            
            logger.info("Netty web server started on {}:{}", host, port);
            logger.info("Access your application at http://{}:{}", host.equals("0.0.0.0") ? "localhost" : host, port);

            serverChannel.closeFuture().await();
        } catch (InterruptedException e) {
            logger.error("Netty server interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
        }
    }

    @Override
    public void stop() {
        shutdown();
    }

    @Override
    public int getPort() {
        return port;
    }

    private void shutdown() {
        logger.info("Shutting down Netty web server...");
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        logger.info("Netty web server stopped");
    }
}
