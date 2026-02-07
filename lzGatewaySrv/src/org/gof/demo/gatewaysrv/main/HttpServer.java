package org.gof.demo.gatewaysrv.main;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.gof.demo.gatewaysrv.connection.ConnectionManager;
import org.gof.demo.gatewaysrv.protocol.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP 网络服务器
 * 处理 HTTP 短连接请求
 */
public class HttpServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private final int port;
    private final ConnectionManager connectionManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public HttpServer(int port, ConnectionManager connectionManager) {
        this.port = port;
        this.connectionManager = connectionManager;
    }

    /**
     * 启动 HTTP 服务器
     */
    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // HTTP 编解码器
                            pipeline.addLast("httpCodec", new HttpServerCodec());

                            // HTTP 对象聚合器
                            pipeline.addLast("httpAggregator",
                                    new HttpObjectAggregator(65536)
                            );

                            // 分块写入处理器
                            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

                            // HTTP 消息处理器
                            pipeline.addLast("httpHandler",
                                    new HttpServerHandler(connectionManager)
                            );
                        }
                    });

            // 绑定端口并启动
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            logger.info("HTTP 服务器启动成功，端口：{}", port);

        } catch (Exception e) {
            logger.error("HTTP 服务器启动失败", e);
            shutdown();
            throw new RuntimeException("HTTP 服务器启动失败", e);
        }
    }

    /**
     * 关闭 HTTP 服务器
     */
    public void shutdown() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        logger.info("HTTP 服务器已关闭");
    }

    /**
     * HTTP 服务器处理器
     */
    private static class HttpServerHandler extends
            io.netty.channel.SimpleChannelInboundHandler<io.netty.handler.codec.http.FullHttpRequest> {

        private static final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);

        private final ConnectionManager connectionManager;
        private HttpHandler protocolHandler;

        public HttpServerHandler(ConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
            this.protocolHandler = new HttpHandler();
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx,
                                     io.netty.handler.codec.http.FullHttpRequest request) {
            String remoteAddress = ctx.channel().remoteAddress().toString();

            // HTTP 是短连接，每次请求都创建临时连接 ID
            long connId = System.currentTimeMillis();

            logger.debug("HTTP 请求：connId={}, method={}, uri={}",
                    connId, request.method(), request.uri());

            // TODO: 获取连接对象并处理消息
            // Connection conn = new Connection(connId, Connection.ConnectionType.HTTP, remoteAddress);
            // protocolHandler.handleMessage(ctx, conn, request);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) {
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("HTTP 连接异常", cause);
            ctx.close();
        }
    }
}
