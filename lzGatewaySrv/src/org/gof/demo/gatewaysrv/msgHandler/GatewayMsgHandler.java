package org.gof.demo.gatewaysrv.msgHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.gof.core.CallPoint;
import org.gof.core.Port;
import org.gof.core.support.Param;
import org.gof.demo.gatewaysrv.connection.Connection;
import org.gof.demo.gatewaysrv.connection.ConnectionManager;
import org.gof.demo.gatewaysrv.connection.ConnectionState;
import org.gof.demo.gatewaysrv.router.ServiceRouter;
import org.gof.demo.gatewaysrv.security.AntiCheatDetector;
import org.gof.demo.gatewaysrv.security.RateLimiter;
import org.gof.demo.gatewaysrv.security.SignatureChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网关消息处理器
 * 处理客户端消息并转发到后端游戏服务
 */
public class GatewayMsgHandler {

    private static final Logger logger = LoggerFactory.getLogger(GatewayMsgHandler.class);

    private final Port port;
    private final ConnectionManager connManager;
    private final ServiceRouter router;
    private final RateLimiter rateLimiter;
    private final SignatureChecker signatureChecker;
    private final AntiCheatDetector antiCheatDetector;

    public GatewayMsgHandler(Port port,
                             ConnectionManager connManager,
                             ServiceRouter router,
                             RateLimiter rateLimiter,
                             SignatureChecker signatureChecker,
                             AntiCheatDetector antiCheatDetector) {
        this.port = port;
        this.connManager = connManager;
        this.router = router;
        this.rateLimiter = rateLimiter;
        this.signatureChecker = signatureChecker;
        this.antiCheatDetector = antiCheatDetector;
    }

    /**
     * 处理客户端消息
     *
     * @param ctx        Netty 通道上下文
     * @param connId     连接 ID
     * @param msgId      消息 ID
     * @param msgData    消息数据
     */
    public void handleMessage(ChannelHandlerContext ctx, long connId, int msgId, byte[] msgData) {
        try {
            // 1. 获取连接
            Connection conn = connManager.getConnection(connId);
            if (conn == null) {
                logger.warn("连接不存在：connId={}", connId);
                return;
            }

            // 2. 安全检查
            if (!performSecurityChecks(conn, msgId, msgData)) {
                return;
            }

            // 3. 路由消息
            CallPoint targetService = routeMessage(conn, msgId);
            if (targetService == null) {
                logger.warn("消息路由失败：connId={}, msgId={}", connId, msgId);
                return;
            }

            // 4. 转发消息到后端服务
            forwardMessage(conn, targetService, msgId, msgData, ctx);

        } catch (Exception e) {
            logger.error("处理消息异常：connId={}, msgId={}", connId, msgId, e);
        }
    }

    /**
     * 执行安全检查
     */
    private boolean performSecurityChecks(Connection conn, int msgId, byte[] msgData) {
        // 1. 限流检查
        if (!rateLimiter.allowConnection(conn.getId())) {
            logger.warn("连接请求超限：connId={}", conn.getId());
            return false;
        }

        // 2. 防外挂检查（如果已登录）
        if (conn.isLoggedIn()) {
            if (!antiCheatDetector.checkMessage(conn.getHumanId(), msgId, msgData)) {
                logger.warn("检测到作弊行为：connId={}, humanId={}",
                        conn.getId(), conn.getHumanId());
                return false;
            }
        }

        return true;
    }

    /**
     * 路由消息到目标服务
     */
    private CallPoint routeMessage(Connection conn, int msgId) {
        // 根据消息类型路由
        CallPoint target = router.routeMessage(msgId);

        // 如果用户已绑定服务，使用绑定的服务
        if (conn.isLoggedIn() && conn.getBoundNodeId() != null) {
            CallPoint boundPoint = new CallPoint();
            boundPoint.nodeId = conn.getBoundNodeId();
            boundPoint.portId = conn.getBoundPortId();
            return boundPoint;
        }

        return target;
    }

    /**
     * 转发消息到后端服务
     */
    private void forwardMessage(Connection conn, CallPoint target, int msgId, byte[] msgData, ChannelHandlerContext ctx) {
        // 构建消息参数
        Param param = new Param();
        param.put("msgId", msgId);
        param.put("msgData", msgData);
        param.put("connId", conn.getId());
        param.put("remoteAddress", conn.getRemoteAddress());

        if (conn.isLoggedIn()) {
            param.put("humanId", conn.getHumanId());
            param.put("sessionId", conn.getSessionId());
        }

        // 保存 ChannelHandlerContext 用于回调
        param.put("channelContext", ctx);

        // TODO: 实际转发到后端服务
        // 这里需要根据实际的服务调用机制来实现
        // 可能需要使用 RPC 或消息队列

        logger.debug("转发消息：connId={}, msgId={}, target={}",
                conn.getId(), msgId, target.servId);
    }

    /**
     * 处理后端服务的响应消息
     *
     * @param connId     连接 ID
     * @param msgId      消息 ID
     * @param responseData 响应数据
     */
    public void handleResponse(long connId, int msgId, byte[] responseData) {
        Connection conn = connManager.getConnection(connId);
        if (conn == null) {
            logger.warn("连接不存在，无法发送响应：connId={}", connId);
            return;
        }

        // TODO: 发送响应到客户端
        // 需要通过协议适配器发送响应

        logger.debug("发送响应：connId={}, msgId={}", connId, msgId);
    }

    /**
     * 处理玩家登录
     */
    public boolean handleLogin(long connId, long humanId, String sessionId, String token) {
        Connection conn = connManager.getConnection(connId);
        if (conn == null) {
            return false;
        }

        // 1. 验证 token
        if (!signatureChecker.verifyLoginToken(token, humanId, conn.getRemoteAddress())) {
            logger.warn("Token 验证失败：connId={}, humanId={}", connId, humanId);
            return false;
        }

        // 2. 绑定用户
        boolean success = connManager.bindHuman(connId, humanId, sessionId);
        if (!success) {
            return false;
        }

        // 3. 获取用户绑定的游戏服务
        CallPoint userService = router.getUserService(humanId);
        if (userService != null) {
            connManager.bindService(connId, userService.nodeId, userService.portId);
        }

        logger.info("玩家登录成功：connId={}, humanId={}", connId, humanId);

        return true;
    }

    /**
     * 处理玩家登出
     */
    public void handleLogout(long connId) {
        Connection conn = connManager.getConnection(connId);
        if (conn == null) {
            return;
        }

        if (conn.isLoggedIn()) {
            long humanId = conn.getHumanId();

            // 清理防外挂检测记录
            antiCheatDetector.clearPlayerBehavior(humanId);

            logger.info("玩家登出：connId={}, humanId={}", connId, humanId);
        }

        // 关闭连接
        connManager.closeConnection(connId);
    }

    /**
     * 广播消息到多个连接
     */
    public void broadcast(Long[] connIds, int msgId, byte[] msgData) {
        for (Long connId : connIds) {
            Connection conn = connManager.getConnection(connId);
            if (conn != null && conn.getState() != ConnectionState.CLOSED) {
                // TODO: 发送消息到客户端
                logger.debug("广播消息：connId={}, msgId={}", connId, msgId);
            }
        }
    }
}
