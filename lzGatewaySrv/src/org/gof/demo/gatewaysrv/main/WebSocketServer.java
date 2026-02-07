package org.gof.demo.gatewaysrv.main;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.gof.demo.gatewaysrv.connection.ConnectionManager;
import org.gof.demo.gatewaysrv.protocol.WebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket 网络服务器
 * 处理 WebSocket 连接
 */
public class WebSocketServer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServer.class);

    private final int port;
    private final String websocketPath;
    private final ConnectionManager connectionManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public WebSocketServer(int port, String websocketPath, ConnectionManager connectionManager) {
        this.port = port;
        this.websocketPath = websocketPath;
        this.connectionManager = connectionManager;
    }

    /**
     * 启动 WebSocket 服务器
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

                            // HTTP 对象聚合器（将多个 HttpContent 聚合成完整的 FullHttpRequest）
                            pipeline.addLast("httpAggregator", new HttpObjectAggregator(65536));

                            // 分块写入处理器（用于大文件传输）
                            pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());

                            // WebSocket 协议处理器
                            pipeline.addLast("websocketProtocol",
                                    new WebSocketServerProtocolHandler(websocketPath)
                            );

                            // WebSocket 消息处理器
                            pipeline.addLast("websocketHandler",
                                    new WebSocketServerHandler(connectionManager)
                            );
                        }
                    });

            // 绑定端口并启动
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            logger.info("WebSocket 服务器启动成功，端口：{}, 路径：{}", port, websocketPath);

        } catch (Exception e) {
            logger.error("WebSocket 服务器启动失败", e);
            shutdown();
            throw new RuntimeException("WebSocket 服务器启动失败", e);
        }
    }

    /**
     * 关闭 WebSocket 服务器
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
        logger.info("WebSocket 服务器已关闭");
    }

    /**
     * WebSocket 服务器处理器
     */
    private static class WebSocketServerHandler extends
            io.netty.channel.SimpleChannelInboundHandler<io.netty.handler.codec.http.websocketx.WebSocketFrame> {

        private static final Logger logger = LoggerFactory.getLogger(WebSocketServerHandler.class);

        private final ConnectionManager connectionManager;
        private WebSocketHandler protocolHandler;
        private long connId = -1;

        public WebSocketServerHandler(ConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
            this.protocolHandler = new WebSocketHandler();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if ("io.netty.handler.codec.http.websocketx.HandshakeComplete".equals(evt.getClass().getName())) {
                String remoteAddress = ctx.channel().remoteAddress().toString();

                // WebSocket 握手完成，创建连接
                connId = connectionManager.createConnection("WEBSOCKET", remoteAddress);

                if (connId > 0) {
                    logger.info("WebSocket 连接建立：connId={}, remoteAddress={}", connId, remoteAddress);
                } else {
                    logger.warn("WebSocket 连接被拒绝：remoteAddress={}", remoteAddress);
                    ctx.close();
                }
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx,
                                    io.netty.handler.codec.http.websocketx.WebSocketFrame frame) {
            if (connId <= 0) {
                logger.warn("无效的连接 ID，拒绝消息");
                return;
            }

            // 更新活跃时间
            connectionManager.updateActiveTime(connId);

            // TODO: 获取连接对象并处理消息
            // Connection conn = connectionManager.getConnection(connId);
            // protocolHandler.handleMessage(ctx, conn, frame);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (connId > 0) {
                logger.info("WebSocket 连接断开：connId={}", connId);
                connectionManager.closeConnection(connId);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("WebSocket 连接异常：connId={}", connId, cause);
            ctx.close();
        }
    }
}
