package org.gof.demo.gatewaysrv.connection;

import org.gof.core.Port;
import org.gof.core.Service;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.gof.core.support.Param;
import org.gof.demo.gatewaysrv.support.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 连接管理器
 * 管理所有客户端连接
 */
@DistrClass
public class ConnectionManager extends Service {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    /** 连接ID生成器 */
    private final AtomicLong connIdGenerator;

    /** 所有连接映射：connId -> Connection */
    private final Map<Long, Connection> connections;

    /** 用户ID到连接的映射：humanId -> connId */
    private final Map<Long, Long> humanToConn;

    /** 会话ID到连接的映射：sessionId -> connId */
    private final Map<String, Long> sessionToConn;

    /** 远程地址到连接计数：remoteAddress -> count (用于检测多开) */
    private final Map<String, Integer> addressCounter;

    /**
     * 构造函数
     */
    public ConnectionManager(Port port) {
        super(port);
        this.connIdGenerator = new AtomicLong(0);
        this.connections = new ConcurrentHashMap<>();
        this.humanToConn = new ConcurrentHashMap<>();
        this.sessionToConn = new ConcurrentHashMap<>();
        this.addressCounter = new ConcurrentHashMap<>();
    }

    @Override
    public Object getId() {
        return GatewayConfig.SERV_GATEWAY_CONN;
    }

    /**
     * 创建新连接
     */
    @DistrMethod
    public long createConnection(String typeStr, String remoteAddress) {
        // 检查连接数限制
        if (connections.size() >= GatewayConfig.MAX_CONNECTIONS()) {
            logger.warn("连接数已达上限：{}", GatewayConfig.MAX_CONNECTIONS());
            return -1;
        }

        // 检查IP连接数限制（可选）
        int addressCount = addressCounter.getOrDefault(remoteAddress, 0);
        if (addressCount >= 10) {
            logger.warn("IP连接数超限：{}", remoteAddress);
            return -1;
        }

        // 解析连接类型
        Connection.ConnectionType type;
        try {
            type = Connection.ConnectionType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            logger.error("未知的连接类型：{}", typeStr);
            return -1;
        }

        // 生成连接ID
        long connId = connIdGenerator.incrementAndGet();

        // 创建连接对象
        Connection conn = new Connection(connId, type, remoteAddress);
        connections.put(connId, conn);

        // 更新地址计数
        addressCounter.merge(remoteAddress, 1, Integer::sum);

        logger.info("新连接创建：{}", conn);

        return connId;
    }

    /**
     * 关闭连接
     */
    @DistrMethod
    public void closeConnection(long connId) {
        Connection conn = connections.remove(connId);
        if (conn == null) {
            return;
        }

        // 清理用户映射
        if (conn.isLoggedIn()) {
            humanToConn.remove(conn.getHumanId());
            if (conn.getSessionId() != null) {
                sessionToConn.remove(conn.getSessionId());
            }
        }

        // 更新地址计数
        String remoteAddress = conn.getRemoteAddress();
        addressCounter.computeIfPresent(remoteAddress, (k, v) -> v > 1 ? v - 1 : null);

        // 关闭连接
        conn.close();

        logger.info("连接关闭：{}", conn);
    }

    /**
     * 绑定用户
     */
    @DistrMethod
    public boolean bindHuman(long connId, long humanId, String sessionId) {
        Connection conn = connections.get(connId);
        if (conn == null) {
            logger.warn("连接不存在：{}", connId);
            return false;
        }

        // 检查是否已有其他连接绑定此用户
        Long oldConnId = humanToConn.get(humanId);
        if (oldConnId != null && oldConnId != connId) {
            logger.warn("用户已有其他连接：humanId={}, oldConnId={}", humanId, oldConnId);
            // 关闭旧连接（踢人）
            closeConnection(oldConnId);
        }

        // 绑定用户
        conn.bindHuman(humanId, sessionId);
        conn.setState(ConnectionState.AUTHENTICATED);

        // 更新映射
        humanToConn.put(humanId, connId);
        if (sessionId != null) {
            sessionToConn.put(sessionId, connId);
        }

        logger.info("用户绑定成功：connId={}, humanId={}, sessionId={}", connId, humanId, sessionId);

        return true;
    }

    /**
     * 绑定游戏服务
     */
    @DistrMethod
    public boolean bindService(long connId, String nodeId, String portId) {
        Connection conn = connections.get(connId);
        if (conn == null) {
            logger.warn("连接不存在：{}", connId);
            return false;
        }

        conn.bindService(nodeId, portId);
        conn.setState(ConnectionState.BOUND);

        logger.info("服务绑定成功：connId={}, nodeId={}, portId={}", connId, nodeId, portId);

        return true;
    }

    /**
     * 获取连接
     */
    @DistrMethod
    public Connection getConnection(long connId) {
        return connections.get(connId);
    }

    /**
     * 根据用户ID获取连接
     */
    @DistrMethod
    public Connection getConnectionByHuman(long humanId) {
        Long connId = humanToConn.get(humanId);
        if (connId == null) {
            return null;
        }
        return connections.get(connId);
    }

    /**
     * 根据会话ID获取连接
     */
    @DistrMethod
    public Connection getConnectionBySession(String sessionId) {
        Long connId = sessionToConn.get(sessionId);
        if (connId == null) {
            return null;
        }
        return connections.get(connId);
    }

    /**
     * 更新连接活跃时间
     */
    @DistrMethod
    public void updateActiveTime(long connId) {
        Connection conn = connections.get(connId);
        if (conn != null) {
            conn.updateActiveTime();
        }
    }

    /**
     * 检查并处理超时连接
     */
    @DistrMethod
    public void checkTimeoutConnections() {
        long timeout = 5 * 60 * 1000; // 5分钟超时

        for (Connection conn : connections.values()) {
            // 检查空闲时间
            if (conn.getIdleTime() > timeout) {
                logger.warn("连接超时关闭：{}", conn);
                closeConnection(conn.getId());
            }
        }
    }

    /**
     * 获取连接统计信息
     */
    @DistrMethod
    public Param getStatistics() {
        Param param = new Param();
        param.put("totalConnections", connections.size());
        param.put("authenticatedUsers", humanToConn.size());

        // 按状态统计
        long connectedCount = connections.values().stream()
                .filter(c -> c.getState() == ConnectionState.CONNECTED)
                .count();
        long authenticatedCount = connections.values().stream()
                .filter(c -> c.getState() == ConnectionState.AUTHENTICATED)
                .count();
        long boundCount = connections.values().stream()
                .filter(c -> c.getState() == ConnectionState.BOUND)
                .count();

        param.put("connectedCount", connectedCount);
        param.put("authenticatedCount", authenticatedCount);
        param.put("boundCount", boundCount);

        return param;
    }

    /**
     * 心跳处理
     */
    @Override
    public void pulseOverride() {
        // 定期检查超时连接
        checkTimeoutConnections();
    }
}
