package org.gof.demo.gatewaysrv.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.gof.demo.gatewaysrv.connection.Connection;
import org.gof.demo.gatewaysrv.connection.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP协议处理器
 * 处理TCP长连接消息
 */
public class TcpHandler implements ProtocolAdapter {

    private static final Logger logger = LoggerFactory.getLogger(TcpHandler.class);

    /** 消息长度头字节数 */
    private static final int LENGTH_HEADER_SIZE = 4;

    /** 消息ID头字节数 */
    private static final int MSG_ID_HEADER_SIZE = 4;

    @Override
    public ProtocolType getType() {
        return ProtocolType.TCP;
    }

    @Override
    public boolean handleMessage(ChannelHandlerContext ctx, Connection conn, Object msg) {
        if (!(msg instanceof ByteBuf)) {
            logger.warn("非ByteBuf消息类型：{}", msg.getClass());
            return false;
        }

        ByteBuf buf = (ByteBuf) msg;

        try {
            // 检查最小长度
            if (buf.readableBytes() < LENGTH_HEADER_SIZE + MSG_ID_HEADER_SIZE) {
                logger.warn("消息长度不足");
                return false;
            }

            // 读取消息长度
            int length = buf.readInt();
            if (length <= 0 || length > 1024 * 1024) { // 最大1MB
                logger.warn("消息长度异常：{}", length);
                return false;
            }

            // 读取消息ID
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
            // 这里应该调用 Router 或 MessageHandler 进行消息转发

            logger.debug("TCP消息处理：connId={}, msgId={}, length={}", conn.getId(), msgId, length);

            return true;

        } catch (Exception e) {
            logger.error("TCP消息处理异常", e);
            return false;
        } finally {
            buf.release();
        }
    }

    @Override
    public void sendMessage(ChannelHandlerContext ctx, Connection conn, Object msg) {
        if (conn.getState() == ConnectionState.CLOSED) {
            logger.warn("连接已关闭，无法发送：connId={}", conn.getId());
            return;
        }

        ByteBuf buf = encode(msg);
        if (buf != null) {
            ctx.writeAndFlush(buf);
        }
    }

    @Override
    public ByteBuf encode(Object msg) {
        // TODO: 实现消息编码
        // 这里需要根据实际的消息格式进行编码
        return null;
    }

    @Override
    public void handleException(ChannelHandlerContext ctx, Connection conn, Throwable cause) {
        logger.error("TCP连接异常：connId={}", conn.getId(), cause);
        ctx.close();
    }

    @Override
    public void handleClose(ChannelHandlerContext ctx, Connection conn) {
        logger.info("TCP连接关闭：connId={}", conn.getId());
        // TODO: 通知 ConnectionManager 清理连接
    }
}
