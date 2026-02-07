package org.gof.demo.gatewaysrv.router;

import org.gof.core.CallPoint;
import org.gof.demo.gatewaysrv.router.ServiceRouter.ServiceInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载均衡器
 * 实现多种负载均衡策略
 */
public class LoadBalancer {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalancer.class);

    /** 随机数生成器 */
    private final Random random;

    /** 轮询计数器 */
    private final AtomicInteger roundRobinCounter;

    /**
     * 负载均衡策略
     */
    public enum Strategy {
        /** 随机 */
        RANDOM,
        /** 轮询 */
        ROUND_ROBIN,
        /** 最少连接 */
        LEAST_CONNECTIONS,
        /** 加权随机 */
        WEIGHTED_RANDOM,
        /** 加权轮询 */
        WEIGHTED_ROUND_ROBIN
    }

    /** 当前策略 */
    private volatile Strategy strategy = Strategy.LEAST_CONNECTIONS;

    /**
     * 构造函数
     */
    public LoadBalancer() {
        this.random = new Random();
        this.roundRobinCounter = new AtomicInteger(0);
    }

    /**
     * 设置负载均衡策略
     */
    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
        logger.info("负载均衡策略更新：{}", strategy);
    }

    /**
     * 选择服务
     */
    public CallPoint selectService(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }

        if (instances.size() == 1) {
            ServiceInstance instance = instances.get(0);
            return instance.toCallPoint();
        }

        return switch (strategy) {
            case RANDOM -> selectRandom(instances);
            case ROUND_ROBIN -> selectRoundRobin(instances);
            case LEAST_CONNECTIONS -> selectLeastConnections(instances);
            case WEIGHTED_RANDOM -> selectWeightedRandom(instances);
            case WEIGHTED_ROUND_ROBIN -> selectWeightedRoundRobin(instances);
        };
    }

    /**
     * 随机选择
     */
    private CallPoint selectRandom(List<ServiceInstance> instances) {
        int index = random.nextInt(instances.size());
        return instances.get(index).toCallPoint();
    }

    /**
     * 轮询选择
     */
    private CallPoint selectRoundRobin(List<ServiceInstance> instances) {
        int index = Math.abs(roundRobinCounter.getAndIncrement()) % instances.size();
        return instances.get(index).toCallPoint();
    }

    /**
     * 最少连接选择
     */
    private CallPoint selectLeastConnections(List<ServiceInstance> instances) {
        ServiceInstance selected = instances.get(0);
        int minConnections = selected.getConnections();

        for (int i = 1; i < instances.size(); i++) {
            ServiceInstance instance = instances.get(i);
            if (instance.getConnections() < minConnections) {
                minConnections = instance.getConnections();
                selected = instance;
            }
        }

        return selected.toCallPoint();
    }

    /**
     * 加权随机选择
     */
    private CallPoint selectWeightedRandom(List<ServiceInstance> instances) {
        // 计算总权重
        int totalWeight = 0;
        for (ServiceInstance instance : instances) {
            totalWeight += instance.getWeight();
        }

        // 生成随机数
        int randomValue = random.nextInt(totalWeight);

        // 根据权重选择
        int currentWeight = 0;
        for (ServiceInstance instance : instances) {
            currentWeight += instance.getWeight();
            if (randomValue < currentWeight) {
                return instance.toCallPoint();
            }
        }

        // 兜底返回最后一个
        return instances.get(instances.size() - 1).toCallPoint();
    }

    /**
     * 加权轮询选择
     */
    private CallPoint selectWeightedRoundRobin(List<ServiceInstance> instances) {
        // 计算总权重
        int totalWeight = 0;
        for (ServiceInstance instance : instances) {
            totalWeight += instance.getWeight();
        }

        // 获取当前计数值
        int counter = Math.abs(roundRobinCounter.getAndIncrement()) % totalWeight;

        // 根据权重选择
        int currentWeight = 0;
        for (ServiceInstance instance : instances) {
            currentWeight += instance.getWeight();
            if (counter < currentWeight) {
                return instance.toCallPoint();
            }
        }

        // 兜底返回最后一个
        return instances.get(instances.size() - 1).toCallPoint();
    }

    /**
     * 获取当前策略
     */
    public Strategy getStrategy() {
        return strategy;
    }
}
