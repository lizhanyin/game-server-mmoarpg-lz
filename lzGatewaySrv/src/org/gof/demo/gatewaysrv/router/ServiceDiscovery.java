package org.gof.demo.gatewaysrv.router;

import org.gof.core.Port;
import org.gof.core.Service;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 服务发现
 * 自动发现和监控后端服务
 */
@DistrClass
public class ServiceDiscovery extends Service {

    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);

    /** 服务实例缓存：serviceId -> ServiceEndpoint */
    private final Map<String, ServiceEndpoint> serviceEndpoints;

    /** 定时任务线程池 */
    private final ScheduledExecutorService scheduler;

    /** 服务路由器引用 */
    private ServiceRouter router;

    /**
     * 服务端点
     */
    public static class ServiceEndpoint {
        /** 服务ID */
        private final String serviceId;
        /** 主机地址 */
        private final String host;
        /** 端口 */
        private final int port;
        /** 服务类型 */
        private final String serviceType;
        /** 健康状态 */
        private volatile boolean healthy;
        /** 最后检查时间 */
        private volatile long lastCheckTime;

        public ServiceEndpoint(String serviceId, String host, int port, String serviceType) {
            this.serviceId = serviceId;
            this.host = host;
            this.port = port;
            this.serviceType = serviceType;
            this.healthy = true;
            this.lastCheckTime = System.currentTimeMillis();
        }

        public String getServiceId() {
            return serviceId;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public String getServiceType() {
            return serviceType;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
            this.lastCheckTime = System.currentTimeMillis();
        }
    }

    /**
     * 构造函数
     */
    public ServiceDiscovery(Port port) {
        super(port);
        this.serviceEndpoints = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    @Override
    public Object getId() {
        return "gateway.servicediscovery";
    }

    /**
     * 设置服务路由器
     */
    public void setRouter(ServiceRouter router) {
        this.router = router;
    }

    /**
     * 启动服务发现
     */
    public void init() {
        // 启动定时健康检查
        scheduler.scheduleAtFixedRate(
                this::healthCheck,
                10, // 初始延迟10秒
                30, // 每30秒检查一次
                TimeUnit.SECONDS
        );

        logger.info("服务发现启动");
    }

    /**
     * 注册服务端点
     */
    @DistrMethod
    public void registerEndpoint(String serviceId, String host, int port, String serviceType) {
        ServiceEndpoint endpoint = new ServiceEndpoint(serviceId, host, port, serviceType);
        serviceEndpoints.put(serviceId, endpoint);

        // 注册到路由器
        if (router != null) {
            String nodeId = "node_" + serviceId;
            String portId = "port_" + serviceId;
            router.registerService(serviceId, nodeId, portId, serviceType);
        }

        logger.info("服务端点注册：serviceId={}, host:{}, type={}", serviceId, host + ":" + port, serviceType);
    }

    /**
     * 注销服务端点
     */
    @DistrMethod
    public void unregisterEndpoint(String serviceId) {
        ServiceEndpoint removed = serviceEndpoints.remove(serviceId);
        if (removed != null) {
            // 从路由器注销
            if (router != null) {
                router.unregisterService(serviceId);
            }
            logger.info("服务端点注销：serviceId={}", serviceId);
        }
    }

    /**
     * 获取服务端点
     */
    @DistrMethod
    public ServiceEndpoint getEndpoint(String serviceId) {
        return serviceEndpoints.get(serviceId);
    }

    /**
     * 获取指定类型的所有健康服务
     */
    @DistrMethod
    public List<String> getHealthyServices(String serviceType) {
        List<String> services = new ArrayList<>();
        for (ServiceEndpoint endpoint : serviceEndpoints.values()) {
            if (endpoint.isHealthy() && serviceType.equals(endpoint.getServiceType())) {
                services.add(endpoint.getServiceId());
            }
        }
        return services;
    }

    /**
     * 健康检查
     */
    @DistrMethod
    public void healthCheck() {
        logger.debug("开始服务健康检查，总数：{}", serviceEndpoints.size());

        for (ServiceEndpoint endpoint : serviceEndpoints.values()) {
            boolean healthy = checkEndpointHealth(endpoint);

            if (healthy != endpoint.isHealthy()) {
                endpoint.setHealthy(healthy);
                logger.info("服务健康状态变化：serviceId={}, healthy={}",
                        endpoint.getServiceId(), healthy);

                // 更新路由器中的服务状态
                if (router != null) {
                    router.setServiceAvailable(endpoint.getServiceId(), healthy);
                }
            }
        }
    }

    /**
     * 检查单个端点健康
     */
    private boolean checkEndpointHealth(ServiceEndpoint endpoint) {
        // TODO: 实现实际的健康检查逻辑
        // 可以通过以下方式检查：
        // 1. TCP连接检查
        // 2. HTTP健康检查接口
        // 3. 心跳检测
        // 4. 服务调用测试

        // 简化实现：随机返回健康状态
        return Math.random() > 0.1; // 90%概率健康
    }

    /**
     * 从配置文件加载服务配置
     */
    @DistrMethod
    public void loadFromConfig() {
        // TODO: 从配置文件或服务注册中心加载服务列表
        logger.info("从配置加载服务列表");
    }

    /**
     * 获取所有服务端点
     */
    @DistrMethod
    public List<ServiceEndpoint> getAllEndpoints() {
        return new ArrayList<>(serviceEndpoints.values());
    }

    /**
     * 清理所有服务端点
     */
    @DistrMethod
    public void clear() {
        int size = serviceEndpoints.size();
        serviceEndpoints.clear();
        logger.info("清空所有服务端点：count={}", size);
    }

    /**
     * 停止服务发现
     */
    public void delete() {
        scheduler.shutdown();
        logger.info("服务发现停止");
    }
}
