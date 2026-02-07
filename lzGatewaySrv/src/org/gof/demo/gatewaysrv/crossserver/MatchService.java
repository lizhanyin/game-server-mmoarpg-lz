package org.gof.demo.gatewaysrv.crossserver;

import org.gof.core.Port;
import org.gof.core.Service;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.gof.core.support.Param;
import org.gof.demo.gatewaysrv.support.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 跨服匹配服务
 * 实现跨服务器玩家匹配功能
 */
@DistrClass
public class MatchService extends Service {

    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);

    /** 匹配类型 */
    public enum MatchType {
        /** 竞技场 */
        ARENA(1, 2),
        /** 副本 */
        DUNGEON(2, 4),
        /** 战斗 */
        BATTLE(3, 10),
        /** 活动 */
        ACTIVITY(4, 20);

        private final int type;
        private final int maxPlayers;

        MatchType(int type, int maxPlayers) {
            this.type = type;
            this.maxPlayers = maxPlayers;
        }

        public int getType() {
            return type;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }
    }

    /** 匹配请求 */
    public static class MatchRequest {
        /** 请求ID */
        private final long requestId;
        /** 玩家ID */
        private final long humanId;
        /** 玩家等级 */
        private final int level;
        /** 战斗力 */
        private final long combatPower;
        /** 匹配类型 */
        private final MatchType matchType;
        /** 服务器ID */
        private final String serverId;
        /** 请求时间 */
        private final long requestTime;

        public MatchRequest(long humanId, int level, long combatPower, MatchType matchType, String serverId) {
            this.requestId = generateRequestId();
            this.humanId = humanId;
            this.level = level;
            this.combatPower = combatPower;
            this.matchType = matchType;
            this.serverId = serverId;
            this.requestTime = System.currentTimeMillis();
        }

        private static long requestIdCounter = 0;

        private static long generateRequestId() {
            return ++requestIdCounter;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - requestTime > GatewayConfig.MATCH_TIMEOUT() * 1000L;
        }
    }

    /** 匹配池：MatchType -> List<MatchRequest> */
    private final Map<MatchType, Queue<MatchRequest>> matchPools;

    /** 玩家匹配状态：humanId -> MatchRequest */
    private final Map<Long, MatchRequest> playerMatches;

    /** 匹配结果回调 */
    private final Map<Long, Param> matchCallbacks;

    /**
     * 构造函数
     */
    public MatchService(Port port) {
        super(port);
        this.matchPools = new ConcurrentHashMap<>();
        this.playerMatches = new ConcurrentHashMap<>();
        this.matchCallbacks = new ConcurrentHashMap<>();

        // 初始化匹配池
        for (MatchType type : MatchType.values()) {
            matchPools.put(type, new LinkedBlockingQueue<>());
        }
    }

    @Override
    public Object getId() {
        return GatewayConfig.SERV_GATEWAY_MATCH;
    }

    /**
     * 加入匹配
     */
    @DistrMethod
    public long joinMatch(long humanId, int level, long combatPower, int matchType, String serverId) {
        if (!GatewayConfig.ENABLE_CROSS_SERVER_MATCH()) {
            logger.warn("跨服匹配未启用");
            return -1;
        }

        // 检查玩家是否已在匹配中
        if (playerMatches.containsKey(humanId)) {
            logger.warn("玩家已在匹配中：humanId={}", humanId);
            return -1;
        }

        MatchType type = Arrays.stream(MatchType.values())
                .filter(t -> t.getType() == matchType)
                .findFirst()
                .orElse(null);

        if (type == null) {
            logger.warn("无效的匹配类型：matchType={}", matchType);
            return -1;
        }

        MatchRequest request = new MatchRequest(humanId, level, combatPower, type, serverId);
        matchPools.get(type).offer(request);
        playerMatches.put(humanId, request);

        logger.info("玩家加入匹配：humanId={}, level={}, combatPower={}, matchType={}",
                humanId, level, combatPower, matchType);

        return request.requestId;
    }

    /**
     * 取消匹配
     */
    @DistrMethod
    public boolean cancelMatch(long humanId) {
        MatchRequest request = playerMatches.remove(humanId);
        if (request == null) {
            logger.warn("玩家不在匹配中：humanId={}", humanId);
            return false;
        }

        Queue<MatchRequest> pool = matchPools.get(request.matchType);
        pool.remove(request);

        logger.info("玩家取消匹配：humanId={}", humanId);
        return true;
    }

    /**
     * 心跳处理（执行匹配逻辑）
     */
    @Override
    public void pulseOverride() {
        if (!GatewayConfig.ENABLE_CROSS_SERVER_MATCH()) {
            return;
        }

        // 清理过期请求
        cleanupExpiredRequests();

        // 对每种匹配类型执行匹配
        for (MatchType type : MatchType.values()) {
            processMatching(type);
        }
    }

    /**
     * 执行匹配逻辑
     */
    private void processMatching(MatchType type) {
        Queue<MatchRequest> pool = matchPools.get(type);
        if (pool.isEmpty()) {
            return;
        }

        int maxPlayers = type.getMaxPlayers();

        // 收集可匹配的请求
        List<MatchRequest> candidates = new ArrayList<>();
        Iterator<MatchRequest> iterator = pool.iterator();

        while (iterator.hasNext() && candidates.size() < maxPlayers) {
            MatchRequest request = iterator.next();
            if (!request.isExpired()) {
                candidates.add(request);
            }
        }

        // 检查是否满足匹配条件
        if (candidates.size() >= getMinPlayers(type)) {
            // 尝试匹配
            if (tryMatch(candidates)) {
                // 移除已匹配的请求
                pool.removeAll(candidates);
                for (MatchRequest request : candidates) {
                    playerMatches.remove(request.humanId);
                }
            }
        }
    }

    /**
     * 获取最小匹配人数
     */
    private int getMinPlayers(MatchType type) {
        return switch (type) {
            case ARENA, BATTLE -> 2;
            case DUNGEON -> 1;
            case ACTIVITY -> 5;
        };
    }

    /**
     * 尝试匹配玩家
     */
    private boolean tryMatch(List<MatchRequest> candidates) {
        if (candidates.isEmpty()) {
            return false;
        }

        // 检查等级差距
        if (!checkLevelRange(candidates)) {
            return false;
        }

        // 检查战斗力差距
        if (!checkCombatPowerRange(candidates)) {
            return false;
        }

        // 匹配成功，创建房间
        String roomId = createRoom(candidates);

        // 通知所有匹配成功的玩家
        for (MatchRequest request : candidates) {
            notifyMatchSuccess(request, roomId, candidates);
        }

        logger.info("匹配成功：roomId={}, playerCount={}", roomId, candidates.size());
        return true;
    }

    /**
     * 检查等级差距
     */
    private boolean checkLevelRange(List<MatchRequest> candidates) {
        if (candidates.size() < 2) {
            return true;
        }

        int minLevel = candidates.stream().mapToInt(r -> r.level).min().orElse(0);
        int maxLevel = candidates.stream().mapToInt(r -> r.level).max().orElse(0);

        // 等级差距不超过10级
        return (maxLevel - minLevel) <= 10;
    }

    /**
     * 检查战斗力差距
     */
    private boolean checkCombatPowerRange(List<MatchRequest> candidates) {
        if (candidates.size() < 2) {
            return true;
        }

        long minPower = candidates.stream().mapToLong(r -> r.combatPower).min().orElse(0);
        long maxPower = candidates.stream().mapToLong(r -> r.combatPower).max().orElse(0);

        // 战斗力差距不超过20%
        if (minPower == 0) {
            return true;
        }

        double ratio = (double) (maxPower - minPower) / minPower;
        return ratio <= 0.2;
    }

    /**
     * 创建房间
     */
    private String createRoom(List<MatchRequest> candidates) {
        String roomId = "room_" + System.currentTimeMillis();

        // TODO: 实际创建房间逻辑
        // 1. 选择一个战斗服务器
        // 2. 在战斗服务器上创建房间
        // 3. 将玩家分配到房间

        return roomId;
    }

    /**
     * 通知匹配成功
     */
    private void notifyMatchSuccess(MatchRequest request, String roomId, List<MatchRequest> allPlayers) {
        Param result = new Param();
        result.put("success", true);
        result.put("roomId", roomId);
        result.put("matchType", request.matchType.getType());

        // 构建玩家列表
        List<Long> playerIds = new ArrayList<>();
        for (MatchRequest r : allPlayers) {
            playerIds.add(r.humanId);
        }
        result.put("players", playerIds);

        // TODO: 通知玩家匹配成功
        // 可以通过调用玩家所在的服务来推送消息
        logger.info("通知玩家匹配成功：humanId={}, roomId={}", request.humanId, roomId);
    }

    /**
     * 清理过期请求
     */
    private void cleanupExpiredRequests() {
        long now = System.currentTimeMillis();

        for (Map.Entry<Long, MatchRequest> entry : playerMatches.entrySet()) {
            MatchRequest request = entry.getValue();
            if (request.isExpired()) {
                // 移除过期请求
                cancelMatch(entry.getKey());

                // 通知玩家匹配超时
                logger.info("匹配超时：humanId={}", entry.getKey());
            }
        }
    }

    /**
     * 获取匹配池状态
     */
    @DistrMethod
    public Param getMatchPoolStatus() {
        Param param = new Param();

        for (MatchType type : MatchType.values()) {
            Queue<MatchRequest> pool = matchPools.get(type);
            Param typeInfo = new Param();
            typeInfo.put("waitingCount", pool.size());
            typeInfo.put("maxPlayers", type.getMaxPlayers());
            param.put(type.name(), typeInfo);
        }

        param.put("totalMatching", playerMatches.size());

        return param;
    }
}
