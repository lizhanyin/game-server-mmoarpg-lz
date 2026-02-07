package org.gof.demo.gatewaysrv.crossserver;

import org.gof.core.Port;
import org.gof.core.Service;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.gof.core.support.Param;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 跨服房间管理器
 * 管理跨服战斗房间
 */
@DistrClass
public class RoomManager extends Service {

    private static final Logger logger = LoggerFactory.getLogger(RoomManager.class);

    /** 房间ID生成器 */
    private final AtomicLong roomIdGenerator;

    /** 房间映射：roomId -> Room */
    private final Map<String, Room> rooms;

    /** 玩家房间映射：humanId -> roomId */
    private final Map<Long, String> playerRooms;

    /** 服务器房间映射：serverId -> Set<roomId> */
    private final Map<String, Set<String>> serverRooms;

    /**
     * 房间类型
     */
    public enum RoomType {
        /** 竞技场 */
        ARENA(1),
        /** 副本 */
        DUNGEON(2),
        /** 战斗 */
        BATTLE(3),
        /** 活动 */
        ACTIVITY(4);

        private final int type;

        RoomType(int type) {
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public static RoomType fromInt(int type) {
            return Arrays.stream(values())
                    .filter(t -> t.type == type)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * 房间
     */
    public static class Room {
        /** 房间ID */
        private final String roomId;
        /** 房间类型 */
        private final RoomType roomType;
        /** 房间名称 */
        private final String roomName;
        /** 最大人数 */
        private final int maxPlayers;
        /** 所属服务器 */
        private volatile String serverId;
        /** 房间状态 */
        private volatile RoomState state;
        /** 玩家列表 */
        private final Set<Long> players;
        /** 创建时间 */
        private final long createTime;
        /** 开始时间 */
        private volatile long startTime;
        /** 结束时间 */
        private volatile long endTime;
        /** 房间参数 */
        private final Param params;

        public Room(String roomId, RoomType roomType, String roomName, int maxPlayers) {
            this.roomId = roomId;
            this.roomType = roomType;
            this.roomName = roomName;
            this.maxPlayers = maxPlayers;
            this.state = RoomState.WAITING;
            this.players = new HashSet<>();
            this.createTime = System.currentTimeMillis();
            this.params = new Param();
        }

        /** 是否已满 */
        public boolean isFull() {
            return players.size() >= maxPlayers;
        }

        /** 是否已开始 */
        public boolean isStarted() {
            return state == RoomState.STARTED || state == RoomState.ENDED;
        }

        /** 添加玩家 */
        public boolean addPlayer(long humanId) {
            if (isFull()) {
                return false;
            }
            return players.add(humanId);
        }

        /** 移除玩家 */
        public boolean removePlayer(long humanId) {
            return players.remove(humanId);
        }

        /** 是否有玩家 */
        public boolean hasPlayer(long humanId) {
            return players.contains(humanId);
        }

        /** 获取玩家人数 */
        public int getPlayerCount() {
            return players.size();
        }
    }

    /**
     * 房间状态
     */
    public enum RoomState {
        /** 等待中 */
        WAITING,
        /** 已开始 */
        STARTED,
        /** 已结束 */
        ENDED,
        /** 已关闭 */
        CLOSED
    }

    /**
     * 构造函数
     */
    public RoomManager(Port port) {
        super(port);
        this.roomIdGenerator = new AtomicLong(0);
        this.rooms = new ConcurrentHashMap<>();
        this.playerRooms = new ConcurrentHashMap<>();
        this.serverRooms = new ConcurrentHashMap<>();
    }

    @Override
    public Object getId() {
        return "gateway.roommanager";
    }

    /**
     * 创建房间
     */
    @DistrMethod
    public String createRoom(String roomName, int roomType, int maxPlayers, String serverId) {
        RoomType type = RoomType.fromInt(roomType);
        if (type == null) {
            logger.warn("无效的房间类型：roomType={}", roomType);
            return null;
        }

        String roomId = generateRoomId(type);
        Room room = new Room(roomId, type, roomName, maxPlayers);
        room.serverId = serverId;

        rooms.put(roomId, room);

        // 添加到服务器房间映射
        serverRooms.computeIfAbsent(serverId, k -> new HashSet<>()).add(roomId);

        logger.info("创建房间：roomId={}, roomName={}, type={}, serverId={}",
                roomId, roomName, type, serverId);

        return roomId;
    }

    /**
     * 加入房间
     */
    @DistrMethod
    public boolean joinRoom(String roomId, long humanId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            logger.warn("房间不存在：roomId={}", roomId);
            return false;
        }

        if (room.isStarted()) {
            logger.warn("房间已开始，无法加入：roomId={}", roomId);
            return false;
        }

        if (room.isFull()) {
            logger.warn("房间已满：roomId={}", roomId);
            return false;
        }

        // 检查玩家是否已在其他房间
        String oldRoomId = playerRooms.get(humanId);
        if (oldRoomId != null) {
            logger.warn("玩家已在其他房间：humanId={}, oldRoomId={}", humanId, oldRoomId);
            return false;
        }

        if (!room.addPlayer(humanId)) {
            return false;
        }

        playerRooms.put(humanId, roomId);

        logger.info("加入房间：roomId={}, humanId={}", roomId, humanId);

        // TODO: 通知房间内其他玩家

        return true;
    }

    /**
     * 离开房间
     */
    @DistrMethod
    public boolean leaveRoom(String roomId, long humanId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return false;
        }

        if (!room.hasPlayer(humanId)) {
            return false;
        }

        room.removePlayer(humanId);
        playerRooms.remove(humanId);

        logger.info("离开房间：roomId={}, humanId={}", roomId, humanId);

        // TODO: 通知房间内其他玩家

        // 如果房间空了，关闭房间
        if (room.getPlayerCount() == 0) {
            closeRoom(roomId);
        }

        return true;
    }

    /**
     * 开始房间
     */
    @DistrMethod
    public boolean startRoom(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return false;
        }

        if (room.isStarted()) {
            logger.warn("房间已开始：roomId={}", roomId);
            return false;
        }

        room.state = RoomState.STARTED;
        room.startTime = System.currentTimeMillis();

        logger.info("开始房间：roomId={}", roomId);

        // TODO: 通知所有玩家房间开始
        // TODO: 通知战斗服务器开始战斗

        return true;
    }

    /**
     * 结束房间
     */
    @DistrMethod
    public boolean endRoom(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return false;
        }

        if (room.state != RoomState.STARTED) {
            return false;
        }

        room.state = RoomState.ENDED;
        room.endTime = System.currentTimeMillis();

        logger.info("结束房间：roomId={}, duration={}ms",
                roomId, room.endTime - room.startTime);

        // TODO: 通知所有玩家房间结束
        // TODO: 结算战斗结果

        // 延迟关闭房间
        scheduleCloseRoom(roomId, 5000);

        return true;
    }

    /**
     * 关闭房间
     */
    @DistrMethod
    public boolean closeRoom(String roomId) {
        Room room = rooms.remove(roomId);
        if (room == null) {
            return false;
        }

        room.state = RoomState.CLOSED;

        // 清理玩家映射
        for (Long humanId : room.players) {
            playerRooms.remove(humanId);
        }

        // 清理服务器房间映射
        Set<String> serverRoomSet = serverRooms.get(room.serverId);
        if (serverRoomSet != null) {
            serverRoomSet.remove(roomId);
            if (serverRoomSet.isEmpty()) {
                serverRooms.remove(room.serverId);
            }
        }

        logger.info("关闭房间：roomId={}", roomId);

        return true;
    }

    /**
     * 获取房间信息
     */
    @DistrMethod
    public Param getRoomInfo(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return null;
        }

        Param param = new Param();
        param.put("roomId", room.roomId);
        param.put("roomName", room.roomName);
        param.put("roomType", room.roomType.getType());
        param.put("state", room.state.name());
        param.put("maxPlayers", room.maxPlayers);
        param.put("playerCount", room.getPlayerCount());
        param.put("serverId", room.serverId);
        param.put("createTime", room.createTime);

        if (room.startTime > 0) {
            param.put("startTime", room.startTime);
        }
        if (room.endTime > 0) {
            param.put("endTime", room.endTime);
        }

        // 玩家列表
        param.put("players", new ArrayList<>(room.players));

        return param;
    }

    /**
     * 获取玩家的房间
     */
    @DistrMethod
    public String getPlayerRoom(long humanId) {
        return playerRooms.get(humanId);
    }

    /**
     * 获取服务器的房间列表
     */
    @DistrMethod
    public List<String> getServerRooms(String serverId) {
        Set<String> roomSet = serverRooms.get(serverId);
        return roomSet != null ? new ArrayList<>(roomSet) : new ArrayList<>();
    }

    /**
     * 生成房间ID
     */
    private String generateRoomId(RoomType type) {
        long id = roomIdGenerator.incrementAndGet();
        return type.name().toLowerCase() + "_" + id;
    }

    /**
     * 延迟关闭房间
     */
    private void scheduleCloseRoom(String roomId, long delay) {
        // TODO: 实现延迟关闭逻辑
        // 可以使用定时任务
    }

    /**
     * 心跳处理（清理超时房间）
     */
    @Override
    public void pulseOverride() {
        long now = System.currentTimeMillis();
        long timeout = 30 * 60 * 1000; // 30分钟超时

        rooms.entrySet().removeIf(entry -> {
            Room room = entry.getValue();
            // 清理长时间等待且没有玩家的房间
            if (room.state == RoomState.WAITING &&
                    room.getPlayerCount() == 0 &&
                    (now - room.createTime) > timeout) {
                logger.info("清理超时房间：roomId={}", entry.getKey());
                // 清理服务器映射
                Set<String> serverRoomSet = serverRooms.get(room.serverId);
                if (serverRoomSet != null) {
                    serverRoomSet.remove(entry.getKey());
                }
                return true;
            }
            return false;
        });
    }
}
