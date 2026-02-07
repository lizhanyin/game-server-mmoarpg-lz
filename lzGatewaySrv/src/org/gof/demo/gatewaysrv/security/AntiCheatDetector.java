package org.gof.demo.gatewaysrv.security;

import org.gof.core.Port;
import org.gof.core.Service;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.gof.demo.gatewaysrv.support.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 防外挂检测器
 * 检测常见的外挂和作弊行为
 */
@DistrClass
public class AntiCheatDetector extends Service {

    private static final Logger logger = LoggerFactory.getLogger(AntiCheatDetector.class);

    /** 玩家行为记录：humanId -> PlayerBehavior */
    private final ConcurrentHashMap<Long, PlayerBehavior> playerBehaviors;

    /**
     * 玩家行为记录
     */
    private static class PlayerBehavior {
        /** 消息频率记录 */
        private final Map<Integer, Long> messageFrequency;
        /** 最后操作时间 */
        private volatile long lastActionTime;
        /** 异常行为计数 */
        private volatile int anomalyCount;
        /** 创建时间 */
        private final long createTime;

        PlayerBehavior() {
            this.messageFrequency = new HashMap<>();
            this.lastActionTime = System.currentTimeMillis();
            this.anomalyCount = 0;
            this.createTime = System.currentTimeMillis();
        }
    }

    /**
     * 作弊类型
     */
    public enum CheatType {
        /** 速度过快（模拟器/加速器） */
        SPEED_HACK,
        /** 消息频率异常（自动脚本） */
        MESSAGE_FLOOD,
        /** 数据包异常（修改器） */
        PACKET_MODIFY,
        /** 时间异常（加速器） */
        TIME_ANOMALY,
        /** 重复操作（脚本） */
        REPEAT_ACTION
    }

    /**
     * 构造函数
     */
    public AntiCheatDetector(Port port) {
        super(port);
        this.playerBehaviors = new ConcurrentHashMap<>();
    }

    @Override
    public Object getId() {
        return "gateway.anticheat";
    }

    /**
     * 检查玩家消息（主要检测点）
     */
    @DistrMethod
    public boolean checkMessage(long humanId, int msgId, byte[] msgData) {
        if (!GatewayConfig.ENABLE_ANTI_CHEAT()) {
            return true;
        }

        PlayerBehavior behavior = playerBehaviors.computeIfAbsent(humanId, k -> new PlayerBehavior());

        // 1. 检查消息频率
        if (isMessageFlood(behavior, msgId)) {
            logger.warn("检测到消息频率异常：humanId={}, msgId={}", humanId, msgId);
            return false;
        }

        // 2. 检查数据包异常
        if (isPacketAnomalous(msgId, msgData)) {
            logger.warn("检测到数据包异常：humanId={}, msgId={}", humanId, msgId);
            return false;
        }

        // 3. 更新行为记录
        behavior.messageFrequency.put(msgId, System.currentTimeMillis());
        behavior.lastActionTime = System.currentTimeMillis();

        return true;
    }

    /**
     * 检查时间间隔异常（加速器检测）
     */
    @DistrMethod
    public boolean checkTimeInterval(long humanId, int expectedInterval, int actualInterval) {
        if (!GatewayConfig.ENABLE_ANTI_CHEAT()) {
            return true;
        }

        // 允许10%的误差
        if (actualInterval < expectedInterval * 0.9) {
            PlayerBehavior behavior = playerBehaviors.get(humanId);
            if (behavior != null) {
                behavior.anomalyCount++;
                logger.warn("检测到时间异常：humanId={}, expected={}, actual={}",
                        humanId, expectedInterval, actualInterval);

                // 异常次数过多则判定为作弊
                if (behavior.anomalyCount > 5) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 检查移动速度（加速器检测）
     */
    @DistrMethod
    public boolean checkMoveSpeed(long humanId, float distance, int timeMs) {
        if (!GatewayConfig.ENABLE_ANTI_CHEAT()) {
            return true;
        }

        // 计算速度（米/秒）
        float speed = (distance * 1000) / timeMs;

        // 假设正常最大速度为 20 米/秒（约72km/h）
        float maxSpeed = 20.0f;

        if (speed > maxSpeed) {
            logger.warn("检测到速度异常：humanId={}, speed={}, distance={}, timeMs={}",
                    humanId, speed, distance, timeMs);
            return false;
        }

        return true;
    }

    /**
     * 检查资源获取异常（修改器检测）
     */
    @DistrMethod
    public boolean checkResourceGain(long humanId, int resourceType, long amount) {
        if (!GatewayConfig.ENABLE_ANTI_CHEAT()) {
            return true;
        }

        // 定义单次获取资源的上限
        long maxAmount = getMaxResourceAmount(resourceType);

        if (amount > maxAmount) {
            logger.warn("检测到资源获取异常：humanId={}, type={}, amount={}",
                    humanId, resourceType, amount);
            return false;
        }

        return true;
    }

    /**
     * 检查消息频率异常
     */
    private boolean isMessageFlood(PlayerBehavior behavior, int msgId) {
        Long lastTime = behavior.messageFrequency.get(msgId);
        if (lastTime == null) {
            return false;
        }

        long interval = System.currentTimeMillis() - lastTime;

        // 同类型消息间隔不能小于100ms
        if (interval < 100) {
            behavior.anomalyCount++;
            return behavior.anomalyCount > 3;
        }

        return false;
    }

    /**
     * 检查数据包异常
     */
    private boolean isPacketAnomalous(int msgId, byte[] msgData) {
        // 基本检查：数据包长度
        if (msgData == null || msgData.length == 0) {
            return true;
        }

        if (msgData.length > 10240) { // 单包最大10KB
            return true;
        }

        // 可以添加更多检查：
        // - 数据包格式校验
        // - 字段范围校验
        // - 签名校验

        return false;
    }

    /**
     * 获取资源类型的最大单次获取量
     */
    private long getMaxResourceAmount(int resourceType) {
        // 根据资源类型返回不同的上限
        return switch (resourceType) {
            case 1 -> 10000;    // 金币
            case 2 -> 1000;     // 钻石
            case 3 -> 100;      // 道具
            default -> 1000;
        };
    }

    /**
     * 清理玩家行为记录
     */
    @DistrMethod
    public void clearPlayerBehavior(long humanId) {
        playerBehaviors.remove(humanId);
    }

    /**
     * 获取玩家异常计数
     */
    @DistrMethod
    public int getAnomalyCount(long humanId) {
        PlayerBehavior behavior = playerBehaviors.get(humanId);
        return behavior != null ? behavior.anomalyCount : 0;
    }

    /**
     * 重置玩家异常计数
     */
    @DistrMethod
    public void resetAnomalyCount(long humanId) {
        PlayerBehavior behavior = playerBehaviors.get(humanId);
        if (behavior != null) {
            behavior.anomalyCount = 0;
        }
    }

    /**
     * 心跳处理（定期清理过期数据）
     */
    @Override
    public void pulseOverride() {
        long now = System.currentTimeMillis();
        long expireTime = 30 * 60 * 1000; // 30分钟

        playerBehaviors.entrySet().removeIf(entry -> {
            PlayerBehavior behavior = entry.getValue();
            return (now - behavior.lastActionTime) > expireTime;
        });
    }
}
