package org.gof.demo.gatewaysrv.router;

import org.gof.core.CallPoint;
import org.gof.core.Port;
import org.gof.core.Service;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.gof.core.support.Param;
import org.gof.demo.gatewaysrv.support.GatewayConfig;
import org.gof.demo.gatewaysrv.protocol.ProtocolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务路由器
 * 负责将客户端消息路由到后端游戏服务
 */
@DistrClass
public class ServiceRouter extends Service {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRouter.class);

    /** 服务注册表：serviceId -> ServiceInstance */
    private final Map<String, ServiceInstance> serviceRegistry;

    /** 消息路由规则：msgId -> serviceId */
    private final Map<Integer, String> messageRoutes;

    /** 负载均衡器 */
    private final LoadBalancer loadBalancer;

    /**
     * 服务实例
     */
    public static class ServiceInstance {
        /** 服务ID */
        private final String serviceId;
        /** 节点ID */
        private final String nodeId;
        /** 端口ID */
        private final String portId;
        /** 服务类型 */
        private final String serviceType;
        /** 权重 */
        private volatile int weight;
        /** 当前连接数 */
        private volatile int connections;
        /** 状态 */
        private volatile boolean available;
        /** 最后心跳时间 */
        private volatile long lastHeartbeat;

        public ServiceInstance(String serviceId, String nodeId, String portId, String serviceType) {
            this.serviceId = serviceId;
            this.nodeId = nodeId;
            this.portId = portId;
            this.serviceType = serviceType;
            this.weight = 100;
            this.connections = 0;
            this.available = true;
            this.lastHeartbeat = System.currentTimeMillis();
        }

        public CallPoint toCallPoint() {
            CallPoint point = new CallPoint();
            point.nodeId = nodeId;
            point.portId = portId;
            point.servId = serviceId;
            return point;
        }

        public void incrementConnections() {
            connections++;
        }

        public void decrementConnections() {
            if (connections > 0) {
                connections--;
            }
        }

        public int getWeight() {
            return weight;
        }

        public int getConnections() {
            return connections;
        }
    }

    /**
     * 构造函数
     */
    public ServiceRouter(Port port) {
        super(port);
        this.serviceRegistry = new ConcurrentHashMap<>();
        this.messageRoutes = new ConcurrentHashMap<>();
        this.loadBalancer = new LoadBalancer();
    }

    @Override
    public Object getId() {
        return GatewayConfig.SERV_GATEWAY_ROUTER;
    }

    /**
     * 注册服务
     */
    @DistrMethod
    public void registerService(String serviceId, String nodeId, String portId, String serviceType) {
        ServiceInstance instance = new ServiceInstance(serviceId, nodeId, portId, serviceType);
        serviceRegistry.put(serviceId, instance);
        logger.info("服务注册成功：serviceId={}, nodeId={}, portId={}, type={}",
                serviceId, nodeId, portId, serviceType);
    }

    /**
     * 注销服务
     */
    @DistrMethod
    public void unregisterService(String serviceId) {
        ServiceInstance removed = serviceRegistry.remove(serviceId);
        if (removed != null) {
            logger.info("服务注销成功：serviceId={}", serviceId);
        }
    }

    /**
     * 服务心跳
     */
    @DistrMethod
    public void heartbeat(String serviceId) {
        ServiceInstance instance = serviceRegistry.get(serviceId);
        if (instance != null) {
            instance.lastHeartbeat = System.currentTimeMillis();
            instance.available = true;
        }
    }

    /**
     * 设置服务可用状态
     */
    @DistrMethod
    public void setServiceAvailable(String serviceId, boolean available) {
        ServiceInstance instance = serviceRegistry.get(serviceId);
        if (instance != null) {
            instance.available = available;
            logger.info("服务状态更新：serviceId={}, available={}", serviceId, available);
        }
    }

    /**
     * 设置服务权重
     */
    @DistrMethod
    public void setServiceWeight(String serviceId, int weight) {
        ServiceInstance instance = serviceRegistry.get(serviceId);
        if (instance != null) {
            instance.weight = weight;
            logger.info("服务权重更新：serviceId={}, weight={}", serviceId, weight);
        }
    }

    /**
     * 配置消息路由
     */
    @DistrMethod
    public void configureMessageRoute(int msgId, String serviceId) {
        messageRoutes.put(msgId, serviceId);
        logger.info("消息路由配置：msgId={}, serviceId={}", msgId, serviceId);
    }

    /**
     * 路由消息到服务
     */
    @DistrMethod
    public CallPoint routeMessage(int msgId) {
        // 1. 使用协议注册器查找服务类型
        String serviceType = ProtocolRegistry.getInstance().getServiceType(msgId);
        if (serviceType == null) {
            // 尝试从配置的路由表中查找
            String serviceId = messageRoutes.get(msgId);
            if (serviceId != null) {
                ServiceInstance instance = serviceRegistry.get(serviceId);
                if (instance != null && instance.available) {
                    instance.incrementConnections();
                    return instance.toCallPoint();
                }
            }
            // 使用默认服务
            serviceType = "worldsrv";
        }

        // 2. 根据服务类型获取服务实例
        ServiceInstance instance = getServicesByType(serviceType).stream().findFirst().orElse(null);
        if (instance == null || !instance.available) {
            logger.warn("服务不可用：msgId={}, serviceType={}", msgId, serviceType);
            return null;
        }

        // 3. 更新连接数
        instance.incrementConnections();

        // 4. 返回调用点
        return instance.toCallPoint();
    }

    /**
     * 获取用户绑定的服务
     */
    @DistrMethod
    public CallPoint getUserService(long humanId) {
        // TODO: 实现用户到服务的绑定逻辑
        // 可以根据用户ID取模，或者查询用户绑定的服务器

        // 简化实现：使用第一个可用的世界服务
        for (ServiceInstance instance : serviceRegistry.values()) {
            if (instance.available && "worldsrv".equals(instance.serviceType)) {
                instance.incrementConnections();
                return instance.toCallPoint();
            }
        }

        return null;
    }

    /**
     * 获取战斗服务
     */
    @DistrMethod
    public CallPoint getBattleService() {
        return loadBalancer.selectService(getServicesByType("battlesrv"));
    }

    /**
     * 获取世界服务
     */
    @DistrMethod
    public CallPoint getWorldService() {
        return loadBalancer.selectService(getServicesByType("worldsrv"));
    }

    /**
     * 根据类型获取服务列表
     */
    private List<ServiceInstance> getServicesByType(String type) {
        List<ServiceInstance> instances = new ArrayList<>();
        for (ServiceInstance instance : serviceRegistry.values()) {
            if (type.equals(instance.serviceType) && instance.available) {
                instances.add(instance);
            }
        }
        return instances;
    }

    /**
     * 获取所有服务信息
     */
    @DistrMethod
    public Param getServiceInfo() {
        Param param = new Param();
        param.put("totalServices", serviceRegistry.size());

        List<Param> services = new ArrayList<>();
        for (ServiceInstance instance : serviceRegistry.values()) {
            Param info = new Param();
            info.put("serviceId", instance.serviceId);
            info.put("nodeId", instance.nodeId);
            info.put("portId", instance.portId);
            info.put("type", instance.serviceType);
            info.put("weight", instance.weight);
            info.put("connections", instance.connections);
            info.put("available", instance.available);
            services.add(info);
        }

        param.put("services", services);
        return param;
    }

    /**
     * 心跳处理（检查服务健康状态）
     */
    @Override
    public void pulseOverride() {
        long now = System.currentTimeMillis();
        long timeout = 30 * 1000; // 30秒心跳超时

        for (ServiceInstance instance : serviceRegistry.values()) {
            // 检查心跳超时
            if (now - instance.lastHeartbeat > timeout) {
                logger.warn("服务心跳超时：serviceId={}", instance.serviceId);
                instance.available = false;
            }
        }
    }
}
