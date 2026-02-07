package org.gof.demo.gatewaysrv.main;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.gof.demo.gatewaysrv.connection.ConnectionManager;
import org.gof.demo.gatewaysrv.protocol.TcpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP 网络服务器
 * 处理 TCP 长连接
 */
public class TcpServer {

    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

    private final int port;
    private final ConnectionManager connectionManager;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public TcpServer(int port, ConnectionManager connectionManager) {
        this.port = port;
        this.connectionManager = connectionManager;
    }

    /**
     * 启动 TCP 服务器
     */
    public void start() {
        bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(0, NioIoHandler.newFactory());

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

                            // 长度字段解码器 (4字节长度头)
                            pipeline.addLast("frameDecoder",
                                    new LengthFieldBasedFrameDecoder(
                                            1024 * 1024,  // 最大帧长度 1MB
                                            0,             // 长度字段偏移量
                                            4,             // 长度字段长度
                                            0,             // 长度调整值
                                            4              // 初始字节数
                                    )
                            );

                            // 长度字段编码器
                            pipeline.addLast("frameEncoder",
                                    new LengthFieldPrepender(4)
                            );

                            // TCP 消息处理器
                            pipeline.addLast("tcpHandler",
                                    new TcpServerHandler(connectionManager)
                            );
                        }
                    });

            // 绑定端口并启动
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();

            logger.info("TCP 服务器启动成功，端口：{}", port);

        } catch (Exception e) {
            logger.error("TCP 服务器启动失败", e);
            shutdown();
            throw new RuntimeException("TCP 服务器启动失败", e);
        }
    }

    /**
     * 关闭 TCP 服务器
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
        logger.info("TCP 服务器已关闭");
    }

    /**
     * TCP 服务器处理器
     */
    private static class TcpServerHandler extends SimpleChannelInboundHandler<io.netty.buffer.ByteBuf> {

        private static final Logger logger = LoggerFactory.getLogger(TcpServerHandler.class);

        private final ConnectionManager connectionManager;
        private TcpHandler protocolHandler;
        private long connId = -1;

        public TcpServerHandler(ConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
            this.protocolHandler = new TcpHandler();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            String remoteAddress = ctx.channel().remoteAddress().toString();

            // 创建连接
            connId = connectionManager.createConnection("TCP", remoteAddress);

            if (connId > 0) {
                logger.info("TCP 连接建立：connId={}, remoteAddress={}", connId, remoteAddress);
            } else {
                logger.warn("TCP 连接被拒绝：remoteAddress={}", remoteAddress);
                ctx.close();
            }

            super.channelActive(ctx);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, io.netty.buffer.ByteBuf msg) {
            if (connId <= 0) {
                logger.warn("无效的连接 ID，拒绝消息");
                return;
            }

            // 更新活跃时间
            connectionManager.updateActiveTime(connId);

            // TODO: 获取连接对象并处理消息
            // Connection conn = connectionManager.getConnection(connId);
            // protocolHandler.handleMessage(ctx, conn, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            if (connId > 0) {
                logger.info("TCP 连接断开：connId={}", connId);
                connectionManager.closeConnection(connId);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("TCP 连接异常：connId={}", connId, cause);
            ctx.close();
        }
    }
}
