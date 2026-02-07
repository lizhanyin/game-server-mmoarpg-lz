package org.gof.demo.gatewaysrv.support;

/**
 * 网关服务配置
 * 定义服务ID、端口等配置信息
 * 支持从外部配置文件加载
 */
public class GatewayConfig {

    // ========== 服务ID配置（不可变） ==========

    /** 网关连接服务ID */
    public static final String SERV_GATEWAY_CONN = "gateway.conn";

    /** 网关路由服务ID */
    public static final String SERV_GATEWAY_ROUTER = "gateway.router";

    /** 网关跨服匹配服务ID */
    public static final String SERV_GATEWAY_MATCH = "gateway.match";

    /** 网关跨服组队服务ID */
    public static final String SERV_GATEWAY_TEAM = "gateway.team";

    /** 网关节点前缀 */
    public static final String NODE_GATEWAY_PREFIX = "gateway";

    // ========== 可配置项 ==========

    /** TCP端口 */
    private int tcpPort = 8000;

    /** WebSocket端口 */
    private int webSocketPort = 8100;

    /** HTTP端口 */
    private int httpPort = 8200;

    /** 最大连接数 */
    private int maxConnections = 10000;

    /** 每个连接每秒最大请求数 */
    private int maxRequestsPerSecond = 100;

    /** 消息最大长度 (字节) */
    private int maxMessageLength = 1024 * 1024;

    /** 启用黑白名单 */
    private boolean enableBlackWhiteList = true;

    /** 启用限流 */
    private boolean enableRateLimiter = true;

    /** 启用签名验证 */
    private boolean enableSignatureCheck = true;

    /** 启用防外挂检测 */
    private boolean enableAntiCheat = true;

    /** 启用跨服匹配 */
    private boolean enableCrossServerMatch = true;

    /** 启用跨服组队 */
    private boolean enableCrossServerTeam = true;

    /** 匹配超时时间（秒） */
    private int matchTimeout = 30;

    /** 组队最大人数 */
    private int teamMaxSize = 5;

    // ========== 单例实例 ==========

    private static volatile GatewayConfig instance;

    private GatewayConfig() {
    }

    /**
     * 获取配置实例
     */
    public static GatewayConfig getInstance() {
        if (instance == null) {
            synchronized (GatewayConfig.class) {
                if (instance == null) {
                    instance = new GatewayConfig();
                }
            }
        }
        return instance;
    }

    // ========== Getter/Setter ==========

    public int getTcpPort() {
        return tcpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public int getWebSocketPort() {
        return webSocketPort;
    }

    public void setWebSocketPort(int webSocketPort) {
        this.webSocketPort = webSocketPort;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxRequestsPerSecond() {
        return maxRequestsPerSecond;
    }

    public void setMaxRequestsPerSecond(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    public boolean isEnableBlackWhiteList() {
        return enableBlackWhiteList;
    }

    public void setEnableBlackWhiteList(boolean enableBlackWhiteList) {
        this.enableBlackWhiteList = enableBlackWhiteList;
    }

    public boolean isEnableRateLimiter() {
        return enableRateLimiter;
    }

    public void setEnableRateLimiter(boolean enableRateLimiter) {
        this.enableRateLimiter = enableRateLimiter;
    }

    public boolean isEnableSignatureCheck() {
        return enableSignatureCheck;
    }

    public void setEnableSignatureCheck(boolean enableSignatureCheck) {
        this.enableSignatureCheck = enableSignatureCheck;
    }

    public boolean isEnableAntiCheat() {
        return enableAntiCheat;
    }

    public void setEnableAntiCheat(boolean enableAntiCheat) {
        this.enableAntiCheat = enableAntiCheat;
    }

    public boolean isEnableCrossServerMatch() {
        return enableCrossServerMatch;
    }

    public void setEnableCrossServerMatch(boolean enableCrossServerMatch) {
        this.enableCrossServerMatch = enableCrossServerMatch;
    }

    public boolean isEnableCrossServerTeam() {
        return enableCrossServerTeam;
    }

    public void setEnableCrossServerTeam(boolean enableCrossServerTeam) {
        this.enableCrossServerTeam = enableCrossServerTeam;
    }

    public int getMatchTimeout() {
        return matchTimeout;
    }

    public void setMatchTimeout(int matchTimeout) {
        this.matchTimeout = matchTimeout;
    }

    public int getTeamMaxSize() {
        return teamMaxSize;
    }

    public void setTeamMaxSize(int teamMaxSize) {
        this.teamMaxSize = teamMaxSize;
    }

    // ========== 静态访问方法（向后兼容） ==========

    public static int PORT_TCP_PREFIX() {
        return getInstance().getTcpPort();
    }

    public static int PORT_WEBSOCKET_PREFIX() {
        return getInstance().getWebSocketPort();
    }

    public static int PORT_HTTP_PREFIX() {
        return getInstance().getHttpPort();
    }

    public static int MAX_CONNECTIONS() {
        return getInstance().getMaxConnections();
    }

    public static int MAX_REQUESTS_PER_SECOND() {
        return getInstance().getMaxRequestsPerSecond();
    }

    public static int MAX_MESSAGE_LENGTH() {
        return getInstance().getMaxMessageLength();
    }

    public static boolean ENABLE_BLACK_WHITE_LIST() {
        return getInstance().isEnableBlackWhiteList();
    }

    public static boolean ENABLE_RATE_LIMITER() {
        return getInstance().isEnableRateLimiter();
    }

    public static boolean ENABLE_SIGNATURE_CHECK() {
        return getInstance().isEnableSignatureCheck();
    }

    public static boolean ENABLE_ANTI_CHEAT() {
        return getInstance().isEnableAntiCheat();
    }

    public static boolean ENABLE_CROSS_SERVER_MATCH() {
        return getInstance().isEnableCrossServerMatch();
    }

    public static boolean ENABLE_CROSS_SERVER_TEAM() {
        return getInstance().isEnableCrossServerTeam();
    }

    public static int MATCH_TIMEOUT() {
        return getInstance().getMatchTimeout();
    }

    public static int TEAM_MAX_SIZE() {
        return getInstance().getTeamMaxSize();
    }
}
