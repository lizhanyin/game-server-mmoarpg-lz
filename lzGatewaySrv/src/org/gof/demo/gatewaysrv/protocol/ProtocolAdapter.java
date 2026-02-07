package org.gof.demo.gatewaysrv.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.gof.demo.gatewaysrv.connection.Connection;

/**
 * 协议适配器接口
 * 定义各种协议的统一处理接口
 */
public interface ProtocolAdapter {

    /**
     * 协议类型
     */
    enum ProtocolType {
        /** TCP */
        TCP,
        /** WebSocket */
        WEBSOCKET,
        /** HTTP */
        HTTP
    }

    /**
     * 获取协议类型
     */
    ProtocolType getType();

    /**
     * 处理接收到的消息
     *
     * @param ctx        Netty通道上下文
     * @param conn       网关连接
     * @param msg        原始消息
     * @return 是否处理成功
     */
    boolean handleMessage(ChannelHandlerContext ctx, Connection conn, Object msg);

    /**
     * 编码并发送消息到客户端
     *
     * @param ctx        Netty通道上下文
     * @param conn       网关连接
     * @param msg        要发送的消息
     */
    void sendMessage(ChannelHandlerContext ctx, Connection conn, Object msg);

    /**
     * 将消息转换为字节缓冲区
     *
     * @param msg        消息对象
     * @return 字节缓冲区
     */
    ByteBuf encode(Object msg);

    /**
     * 处理异常
     *
     * @param ctx        Netty通道上下文
     * @param conn       网关连接
     * @param cause      异常原因
     */
    void handleException(ChannelHandlerContext ctx, Connection conn, Throwable cause);

    /**
     * 处理连接关闭
     *
     * @param ctx        Netty通道上下文
     * @param conn       网关连接
     */
    void handleClose(ChannelHandlerContext ctx, Connection conn);
}
