package org.gof.demo.gatewaysrv.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.gof.demo.gatewaysrv.connection.Connection;
import org.gof.demo.gatewaysrv.connection.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket协议处理器
 * 处理WebSocket连接消息
 */
public class WebSocketHandler implements ProtocolAdapter {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    @Override
    public ProtocolType getType() {
        return ProtocolType.WEBSOCKET;
    }

    @Override
    public boolean handleMessage(ChannelHandlerContext ctx, Connection conn, Object msg) {
        if (!(msg instanceof WebSocketFrame)) {
            logger.warn("非WebSocketFrame消息类型：{}", msg.getClass());
            return false;
        }

        WebSocketFrame frame = (WebSocketFrame) msg;

        try {
            // 处理二进制帧
            if (frame instanceof BinaryWebSocketFrame) {
                ByteBuf buf = frame.content();

                // 读取消息ID
                if (buf.readableBytes() < 4) {
                    logger.warn("WebSocket消息长度不足");
                    return false;
                }

                int msgId = buf.readInt();

                // 读取消息体
                byte[] body = new byte[buf.readableBytes()];
                buf.readBytes(body);

                // 更新连接活跃时间
                conn.updateActiveTime();

                // 检查请求限流
                if (conn.isRequestExceeded()) {
                    logger.warn("连接请求超限：connId={}, count={}", conn.getId(), conn.getRequestCount());
                    return false;
                }

                // 增加请求计数
                conn.incrementRequest();

                // TODO: 转发到消息处理器进行业务处理

                logger.debug("WebSocket二进制消息：connId={}, msgId={}", conn.getId(), msgId);
            }
            // 处理文本帧（可用于调试）
            else if (frame instanceof TextWebSocketFrame) {
                String text = ((TextWebSocketFrame) frame).text();

                // 更新连接活跃时间
                conn.updateActiveTime();

                logger.debug("WebSocket文本消息：connId={}, text={}", conn.getId(), text);

                // TODO: 处理文本消息（如JSON格式的控制命令）
            }

            return true;

        } catch (Exception e) {
            logger.error("WebSocket消息处理异常", e);
            return false;
        }
    }

    @Override
    public void sendMessage(ChannelHandlerContext ctx, Connection conn, Object msg) {
        if (conn.getState() == ConnectionState.CLOSED) {
            logger.warn("连接已关闭，无法发送：connId={}", conn.getId());
            return;
        }

        if (msg instanceof ByteBuf) {
            // 发送二进制帧
            ctx.writeAndFlush(new BinaryWebSocketFrame((ByteBuf) msg));
        } else if (msg instanceof String) {
            // 发送文本帧
            ctx.writeAndFlush(new TextWebSocketFrame((String) msg));
        } else {
            ByteBuf buf = encode(msg);
            if (buf != null) {
                ctx.writeAndFlush(new BinaryWebSocketFrame(buf));
            }
        }
    }

    @Override
    public ByteBuf encode(Object msg) {
        // TODO: 实现WebSocket消息编码
        return null;
    }

    @Override
    public void handleException(ChannelHandlerContext ctx, Connection conn, Throwable cause) {
        logger.error("WebSocket连接异常：connId={}", conn.getId(), cause);
        ctx.close();
    }

    @Override
    public void handleClose(ChannelHandlerContext ctx, Connection conn) {
        logger.info("WebSocket连接关闭：connId={}", conn.getId());
        // TODO: 通知 ConnectionManager 清理连接
    }
}
