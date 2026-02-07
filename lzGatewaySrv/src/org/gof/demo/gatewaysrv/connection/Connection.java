package org.gof.demo.gatewaysrv.connection;

import org.gof.demo.gatewaysrv.support.GatewayConfig;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 网关连接
 * 表示一个客户端连接
 */
public class Connection {

    /** 连接ID */
    private final long connId;

    /** 连接类型 */
    private final ConnectionType type;

    /** 远程地址 */
    private final String remoteAddress;

    /** 连接时间戳 */
    private final long connectTime;

    /** 最后活跃时间戳 */
    private volatile long lastActiveTime;

    /** 用户ID（登录后绑定） */
    private volatile long humanId;

    /** 会话ID */
    private volatile String sessionId;

    /** 连接状态 */
    private volatile ConnectionState state;

    /** 请求计数器（用于限流） */
    private final AtomicLong requestCounter;

    /** 请求计数器重置时间 */
    private volatile long counterResetTime;

    /** 绑定的服务节点 */
    private volatile String boundNodeId;

    /** 绑定的服务端口 */
    private volatile String boundPortId;

    /** 附加属性 */
    private final Object attachment;

    /**
     * 连接类型枚举
     */
    public enum ConnectionType {
        /** TCP连接 */
        TCP,
        /** WebSocket连接 */
        WEBSOCKET,
        /** HTTP连接 */
        HTTP
    }

    /**
     * 构造函数
     */
    public Connection(long connId, ConnectionType type, String remoteAddress) {
        this.connId = connId;
        this.type = type;
        this.remoteAddress = remoteAddress;
        this.connectTime = System.currentTimeMillis();
        this.lastActiveTime = connectTime;
        this.humanId = 0;
        this.sessionId = null;
        this.state = ConnectionState.CONNECTED;
        this.requestCounter = new AtomicLong(0);
        this.counterResetTime = System.currentTimeMillis();
        this.attachment = new Object();
    }

    /**
     * 获取连接ID
     */
    public long getId() {
        return connId;
    }

    /**
     * 获取连接类型
     */
    public ConnectionType getType() {
        return type;
    }

    /**
     * 获取远程地址
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * 获取连接时长（毫秒）
     */
    public long getDuration() {
        return System.currentTimeMillis() - connectTime;
    }

    /**
     * 获取最后活跃时间戳
     */
    public long getLastActiveTime() {
        return lastActiveTime;
    }

    /**
     * 更新活跃时间
     */
    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 获取空闲时长（毫秒）
     */
    public long getIdleTime() {
        return System.currentTimeMillis() - lastActiveTime;
    }

    /**
     * 绑定用户
     */
    public void bindHuman(long humanId, String sessionId) {
        this.humanId = humanId;
        this.sessionId = sessionId;
    }

    /**
     * 获取用户ID
     */
    public long getHumanId() {
        return humanId;
    }

    /**
     * 获取会话ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 是否已登录
     */
    public boolean isLoggedIn() {
        return humanId > 0 && sessionId != null;
    }

    /**
     * 获取连接状态
     */
    public ConnectionState getState() {
        return state;
    }

    /**
     * 设置连接状态
     */
    public void setState(ConnectionState state) {
        this.state = state;
    }

    /**
     * 增加请求计数
     */
    public long incrementRequest() {
        // 检查是否需要重置计数器（每秒重置一次）
        long now = System.currentTimeMillis();
        if (now - counterResetTime >= 1000) {
            synchronized (this) {
                if (now - counterResetTime >= 1000) {
                    requestCounter.set(0);
                    counterResetTime = now;
                }
            }
        }
        return requestCounter.incrementAndGet();
    }

    /**
     * 获取当前请求数（用于限流检查）
     */
    public long getRequestCount() {
        // 检查是否需要重置计数器
        long now = System.currentTimeMillis();
        if (now - counterResetTime >= 1000) {
            synchronized (this) {
                if (now - counterResetTime >= 1000) {
                    requestCounter.set(0);
                    counterResetTime = now;
                }
            }
        }
        return requestCounter.get();
    }

    /**
     * 是否超过请求限制
     */
    public boolean isRequestExceeded() {
        return getRequestCount() > GatewayConfig.MAX_REQUESTS_PER_SECOND();
    }

    /**
     * 绑定服务节点
     */
    public void bindService(String nodeId, String portId) {
        this.boundNodeId = nodeId;
        this.boundPortId = portId;
    }

    /**
     * 获取绑定的服务节点ID
     */
    public String getBoundNodeId() {
        return boundNodeId;
    }

    /**
     * 获取绑定的服务端口ID
     */
    public String getBoundPortId() {
        return boundPortId;
    }

    /**
     * 获取附加属性对象（用于存储自定义数据）
     */
    public Object getAttachment() {
        return attachment;
    }

    /**
     * 关闭连接
     */
    public void close() {
        this.state = ConnectionState.CLOSED;
    }

    @Override
    public String toString() {
        return "Connection{" +
                "id=" + connId +
                ", type=" + type +
                ", remoteAddress='" + remoteAddress + '\'' +
                ", humanId=" + humanId +
                ", state=" + state +
                ", duration=" + getDuration() +
                '}';
    }
}
