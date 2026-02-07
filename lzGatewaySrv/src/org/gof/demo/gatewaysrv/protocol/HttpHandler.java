package org.gof.demo.gatewaysrv.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.gof.demo.gatewaysrv.connection.Connection;
import org.gof.demo.gatewaysrv.connection.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * HTTP协议处理器
 * 处理HTTP短连接请求
 */
public class HttpHandler implements ProtocolAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HttpHandler.class);

    @Override
    public ProtocolType getType() {
        return ProtocolType.HTTP;
    }

    @Override
    public boolean handleMessage(ChannelHandlerContext ctx, Connection conn, Object msg) {
        if (!(msg instanceof FullHttpRequest)) {
            logger.warn("非HttpRequest消息类型：{}", msg.getClass());
            return false;
        }

        FullHttpRequest request = (FullHttpRequest) msg;

        try {
            String uri = request.uri();
            HttpMethod method = request.method();

            // 更新连接活跃时间
            conn.updateActiveTime();

            // 检查请求限流（HTTP基于IP限流）
            if (conn.isRequestExceeded()) {
                logger.warn("HTTP请求超限：connId={}, uri={}", conn.getId(), uri);
                sendErrorResponse(ctx, HttpResponseStatus.TOO_MANY_REQUESTS);
                return false;
            }

            // 增加请求计数
            conn.incrementRequest();

            logger.debug("HTTP请求：connId={}, method={}, uri={}", conn.getId(), method, uri);

            // 路由处理
            // TODO: 实现HTTP路由，根据URI和方法分发到不同的处理器
            boolean handled = handleHttpRequest(ctx, conn, request);

            return handled;

        } catch (Exception e) {
            logger.error("HTTP消息处理异常", e);
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            return false;
        } finally {
            request.release();
        }
    }

    /**
     * 处理HTTP请求
     */
    private boolean handleHttpRequest(ChannelHandlerContext ctx, Connection conn, FullHttpRequest request) {
        String uri = request.uri();
        HttpMethod method = request.method();

        // 示例：简单的路由
        if (uri.startsWith("/api/")) {
            // API请求
            return handleApiRequest(ctx, conn, request);
        } else if (uri.startsWith("/health")) {
            // 健康检查
            return handleHealthCheck(ctx, conn);
        } else {
            // 404
            sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND);
            return false;
        }
    }

    /**
     * 处理API请求
     */
    private boolean handleApiRequest(ChannelHandlerContext ctx, Connection conn, FullHttpRequest request) {
        // TODO: 解析请求参数、验证签名、转发到后端服务

        // 示例响应
        FullHttpResponse response = createJsonResponse("{\"code\":0,\"msg\":\"success\"}");
        ctx.writeAndFlush(response);
        return true;
    }

    /**
     * 处理健康检查
     */
    private boolean handleHealthCheck(ChannelHandlerContext ctx, Connection conn) {
        FullHttpResponse response = createJsonResponse("{\"status\":\"ok\"}");
        ctx.writeAndFlush(response);
        return true;
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = createJsonResponse(
                "{\"code\":" + status.code() + ",\"msg\":\"" + status.reasonPhrase() + "\"}"
        );
        response.setStatus(status);
        ctx.writeAndFlush(response);
    }

    /**
     * 创建JSON响应
     */
    private FullHttpResponse createJsonResponse(String json) {
        // TODO: 实现创建JSON响应
        return null;
    }

    @Override
    public void sendMessage(ChannelHandlerContext ctx, Connection conn, Object msg) {
        if (conn.getState() == ConnectionState.CLOSED) {
            logger.warn("连接已关闭，无法发送：connId={}", conn.getId());
            return;
        }

        if (msg instanceof FullHttpResponse) {
            ctx.writeAndFlush(msg);
        } else {
            logger.warn("HTTP需要发送FullHttpResponse类型：{}", msg.getClass());
        }
    }

    @Override
    public ByteBuf encode(Object msg) {
        // HTTP响应不使用此方法
        return null;
    }

    @Override
    public void handleException(ChannelHandlerContext ctx, Connection conn, Throwable cause) {
        logger.error("HTTP连接异常：connId={}", conn.getId(), cause);
        sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        ctx.close();
    }

    @Override
    public void handleClose(ChannelHandlerContext ctx, Connection conn) {
        logger.info("HTTP连接关闭：connId={}", conn.getId());
        // TODO: 通知 ConnectionManager 清理连接
    }
}
