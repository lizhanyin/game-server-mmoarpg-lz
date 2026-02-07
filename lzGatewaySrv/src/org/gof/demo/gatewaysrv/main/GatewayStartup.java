package org.gof.demo.gatewaysrv.main;

import org.gof.core.Node;
import org.gof.core.Port;
import org.gof.core.support.Distr;
import org.gof.demo.gatewaysrv.connection.ConnectionManager;
import org.gof.demo.gatewaysrv.crossserver.MatchService;
import org.gof.demo.gatewaysrv.crossserver.RoomManager;
import org.gof.demo.gatewaysrv.crossserver.TeamService;
import org.gof.demo.gatewaysrv.router.ServiceDiscovery;
import org.gof.demo.gatewaysrv.router.ServiceRouter;
import org.gof.demo.gatewaysrv.security.AntiCheatDetector;
import org.gof.demo.gatewaysrv.security.BlackWhiteList;
import org.gof.demo.gatewaysrv.security.RateLimiter;
import org.gof.demo.gatewaysrv.security.SignatureChecker;
import org.gof.demo.gatewaysrv.support.GatewayConfig;
import org.gof.demo.gatewaysrv.support.GatewayConfigLoader;
import org.gof.demo.gatewaysrv.msgHandler.GatewayMsgHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网关服务启动类
 */
public class GatewayStartup {

    private static final Logger logger = LoggerFactory.getLogger(GatewayStartup.class);

    /** 节点实例 */
    private static Node node;

    /** 网关端口 */
    private static Port gatewayPort;

    /** 连接管理器引用 */
    private static ConnectionManager connManager;

    /** TCP 服务器 */
    private static TcpServer tcpServer;

    /** WebSocket 服务器 */
    private static WebSocketServer webSocketServer;

    /** HTTP 服务器 */
    private static HttpServer httpServer;

    /**
     * 主入口
     */
    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   龙之谷网关服务器启动中...");
        logger.info("========================================");

        try {
            // 初始化配置
            initConfig();

            // 创建节点
            createNode();

            // 创建网关端口
            createGatewayPort();

            // 初始化核心服务
            initCoreServices();

            // 初始化安全服务
            initSecurityServices();

            // 初始化路由服务
            initRouterServices();

            // 初始化跨服服务
            initCrossServerServices();

            // 启动网络服务
            startNetworkServices();

            // 启动完成
            logger.info("========================================");
            logger.info("   网关服务器启动完成！");
            logger.info("   节点ID: {}", node.getId());
            logger.info("   端口ID: {}", gatewayPort.getId());
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("网关服务器启动失败", e);
            System.exit(1);
        }
    }

    /**
     * 初始化配置
     */
    private static void initConfig() {
        logger.info("初始化配置...");

        // 设置工作目录
        System.setProperty("workDir", "./");

        // 加载外部配置文件
        GatewayConfigLoader configLoader = new GatewayConfigLoader();
        configLoader.applyConfig();

        logger.info("配置初始化完成");
    }

    /**
     * 创建节点
     */
    private static void createNode() {
        logger.info("创建节点...");

        String nodeId = GatewayConfig.NODE_GATEWAY_PREFIX + "_1";
        String nodeAddr = Distr.getNodeAddr(nodeId);
        node = new Node(nodeId, nodeAddr);

        logger.info("节点创建成功：{}", nodeId);
    }

    /**
     * 创建网关端口
     */
    private static void createGatewayPort() {
        logger.info("创建网关端口...");

        String portId = "gateway_port";
        gatewayPort = new Port(portId) {
            @Override
            protected void pulseOverride() {
                // 网关特定的心跳处理
            }
        };

        gatewayPort.startup(node);

        logger.info("网关端口创建成功：{}", portId);
    }

    /**
     * 初始化核心服务
     */
    private static void initCoreServices() {
        logger.info("初始化核心服务...");

        // 连接管理器
        connManager = new ConnectionManager(gatewayPort);
        connManager.startup();
        logger.info("  - 连接管理器启动：{}", connManager.getId());

        logger.info("核心服务初始化完成");
    }

    /**
     * 初始化安全服务
     */
    private static void initSecurityServices() {
        logger.info("初始化安全服务...");

        // 黑白名单
        if (GatewayConfig.ENABLE_BLACK_WHITE_LIST()) {
            BlackWhiteList blackWhiteList = new BlackWhiteList(gatewayPort);
            blackWhiteList.startup();
            logger.info("  - 黑白名单服务启动");
        }

        // 限流器
        if (GatewayConfig.ENABLE_RATE_LIMITER()) {
            RateLimiter rateLimiter = new RateLimiter(gatewayPort);
            rateLimiter.startup();
            logger.info("  - 限流器启动");
        }

        // 签名校验器
        if (GatewayConfig.ENABLE_SIGNATURE_CHECK()) {
            SignatureChecker signatureChecker = new SignatureChecker(gatewayPort);
            signatureChecker.startup();
            logger.info("  - 签名校验器启动");
        }

        // 防外挂检测器
        if (GatewayConfig.ENABLE_ANTI_CHEAT()) {
            AntiCheatDetector antiCheatDetector = new AntiCheatDetector(gatewayPort);
            antiCheatDetector.startup();
            logger.info("  - 防外挂检测器启动");
        }

        logger.info("安全服务初始化完成");
    }

    /**
     * 初始化路由服务
     */
    private static void initRouterServices() {
        logger.info("初始化路由服务...");

        // 服务路由器
        ServiceRouter router = new ServiceRouter(gatewayPort);
        router.startup();
        logger.info("  - 服务路由器启动");

        // 服务发现
        ServiceDiscovery discovery = new ServiceDiscovery(gatewayPort);
        discovery.setRouter(router);
        discovery.startup();
        logger.info("  - 服务发现启动");

        logger.info("路由服务初始化完成");
    }

    /**
     * 初始化跨服服务
     */
    private static void initCrossServerServices() {
        logger.info("初始化跨服服务...");

        // 跨服匹配
        if (GatewayConfig.ENABLE_CROSS_SERVER_MATCH()) {
            MatchService matchService = new MatchService(gatewayPort);
            matchService.startup();
            logger.info("  - 跨服匹配服务启动");
        }

        // 跨服组队
        if (GatewayConfig.ENABLE_CROSS_SERVER_TEAM()) {
            TeamService teamService = new TeamService(gatewayPort);
            teamService.startup();
            logger.info("  - 跨服组队服务启动");
        }

        // 房间管理
        RoomManager roomManager = new RoomManager(gatewayPort);
        roomManager.startup();
        logger.info("  - 房间管理器启动");

        logger.info("跨服服务初始化完成");
    }

    /**
     * 启动网络服务
     */
    private static void startNetworkServices() {
        logger.info("启动网络服务...");

        try {
            // TCP端口
            int tcpPort = GatewayConfig.PORT_TCP_PREFIX();
            tcpServer = new TcpServer(tcpPort, connManager);
            tcpServer.start();
            logger.info("  - TCP端口：{}", tcpPort);

            // WebSocket端口
            int wsPort = GatewayConfig.PORT_WEBSOCKET_PREFIX();
            webSocketServer = new WebSocketServer(wsPort, "/ws", connManager);
            webSocketServer.start();
            logger.info("  - WebSocket端口：{}", wsPort);

            // HTTP端口
            int httpPort = GatewayConfig.PORT_HTTP_PREFIX();
            httpServer = new HttpServer(httpPort, connManager);
            httpServer.start();
            logger.info("  - HTTP端口：{}", httpPort);

            logger.info("网络服务启动完成");
        } catch (Exception e) {
            logger.error("网络服务启动失败", e);
            throw new RuntimeException("网络服务启动失败", e);
        }
    }

    /**
     * 关闭钩子
     */
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("网关服务器正在关闭...");

            // 关闭网络服务
            if (tcpServer != null) {
                tcpServer.shutdown();
            }
            if (webSocketServer != null) {
                webSocketServer.shutdown();
            }
            if (httpServer != null) {
                httpServer.shutdown();
            }

            if (gatewayPort != null) {
                gatewayPort.stop();
            }

            if (node != null) {
                node.stop();
            }

            logger.info("网关服务器已关闭");
        }));
    }
}
