package org.gof.demo.gatewaysrv.security;

import org.gof.core.Port;
import org.gof.core.Service;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.gof.demo.gatewaysrv.support.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流器
 * 基于令牌桶算法实现限流
 */
@DistrClass
public class RateLimiter extends Service {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

    /** 限流桶：key -> RateLimiterBucket */
    private final ConcurrentHashMap<String, RateLimiterBucket> buckets;

    /**
     * 限流桶
     */
    private static class RateLimiterBucket {
        /** 令牌数 */
        private final AtomicLong tokens;
        /** 最后更新时间 */
        private volatile long lastUpdateTime;
        /** 最大令牌数 */
        private final long maxTokens;
        /** 每秒补充令牌数 */
        private final long refillRate;

        RateLimiterBucket(long maxTokens, long refillRate) {
            this.maxTokens = maxTokens;
            this.refillRate = refillRate;
            this.tokens = new AtomicLong(maxTokens);
            this.lastUpdateTime = System.currentTimeMillis();
        }

        /**
         * 尝试消费令牌
         */
        boolean tryConsume(int count) {
            refill();
            long current = tokens.get();
            if (current >= count) {
                return tokens.compareAndSet(current, current - count);
            }
            return false;
        }

        /**
         * 补充令牌
         */
        void refill() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastUpdateTime;

            if (elapsed >= 1000) {
                long tokensToAdd = (elapsed / 1000) * refillRate;
                long oldTokens = tokens.get();
                long newTokens = Math.min(maxTokens, oldTokens + tokensToAdd);
                tokens.set(newTokens);
                lastUpdateTime = now;
            }
        }

        /**
         * 获取当前令牌数
         */
        long getTokens() {
            refill();
            return tokens.get();
        }
    }

    /**
     * 构造函数
     */
    public RateLimiter(Port port) {
        super(port);
        this.buckets = new ConcurrentHashMap<>();
    }

    @Override
    public Object getId() {
        return "gateway.ratelimiter";
    }

    /**
     * 检查是否允许请求
     *
     * @param key        限流key（可以是IP、用户ID等）
     * @param count      需要消费的令牌数
     * @return 是否允许
     */
    @DistrMethod
    public boolean allowRequest(String key, int count) {
        if (!GatewayConfig.ENABLE_RATE_LIMITER()) {
            return true;
        }

        RateLimiterBucket bucket = buckets.computeIfAbsent(key,
                k -> new RateLimiterBucket(100, 50)); // 默认：100最大令牌，每秒补充50

        boolean allowed = bucket.tryConsume(count);

        if (!allowed) {
            logger.warn("请求被限流：key={}, tokens={}", key, bucket.getTokens());
        }

        return allowed;
    }

    /**
     * 检查IP是否允许请求
     */
    @DistrMethod
    public boolean allowIp(String ip) {
        return allowRequest("ip:" + ip, 1);
    }

    /**
     * 检查用户是否允许请求
     */
    @DistrMethod
    public boolean allowUser(long humanId) {
        return allowRequest("user:" + humanId, 1);
    }

    /**
     * 检查连接是否允许请求
     */
    @DistrMethod
    public boolean allowConnection(long connId) {
        return allowRequest("conn:" + connId, 1);
    }

    /**
     * 设置限流规则
     */
    @DistrMethod
    public void setRateLimit(String key, long maxTokens, long refillRate) {
        RateLimiterBucket bucket = new RateLimiterBucket(maxTokens, refillRate);
        buckets.put(key, bucket);
        logger.info("设置限流规则：key={}, maxTokens={}, refillRate={}", key, maxTokens, refillRate);
    }

    /**
     * 移除限流规则
     */
    @DistrMethod
    public void removeRateLimit(String key) {
        buckets.remove(key);
        logger.info("移除限流规则：key={}", key);
    }

    /**
     * 获取当前令牌数
     */
    @DistrMethod
    public long getTokens(String key) {
        RateLimiterBucket bucket = buckets.get(key);
        return bucket != null ? bucket.getTokens() : 0;
    }

    /**
     * 清空所有限流规则
     */
    @DistrMethod
    public void clear() {
        int size = buckets.size();
        buckets.clear();
        logger.info("清空所有限流规则：count={}", size);
    }

    /**
     * 心跳处理（定期清理过期的限流桶）
     */
    @Override
    public void pulseOverride() {
        // 可以在这里添加清理逻辑，例如长时间未使用的限流桶可以清理
        // 为简化实现，暂时不做处理
    }
}
