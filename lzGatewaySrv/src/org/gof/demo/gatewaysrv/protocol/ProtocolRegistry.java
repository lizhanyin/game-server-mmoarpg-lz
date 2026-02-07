package org.gof.demo.gatewaysrv.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 协议注册器
 * 管理消息ID到后端服务的映射
 */
public class ProtocolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ProtocolRegistry.class);

    /** 单例实例 */
    private static volatile ProtocolRegistry instance;

    /** 消息ID到服务类型的映射 */
    private final Map<Integer, String> messageServiceMap;

    /** 消息ID到消息名称的映射 */
    private final Map<Integer, String> messageNameMap;

    private ProtocolRegistry() {
        this.messageServiceMap = new HashMap<>();
        this.messageNameMap = new HashMap<>();
        registerDefaultMessages();
    }

    /**
     * 获取单例实例
     */
    public static ProtocolRegistry getInstance() {
        if (instance == null) {
            synchronized (ProtocolRegistry.class) {
                if (instance == null) {
                    instance = new ProtocolRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * 注册默认消息映射
     */
    private void registerDefaultMessages() {
        // ========== 登录相关 ==========
        registerMessage(111, "login", "登录服务");
        registerMessage(112, "login", "登录服务");
        registerMessage(121, "login", "账号服务");
        registerMessage(122, "login", "账号服务");

        // ========== 角色相关 ==========
        registerMessage(1003, "human", "角色服务");
        registerMessage(1004, "human", "角色服务");
        registerMessage(1005, "human", "角色服务");
        registerMessage(1006, "human", "角色服务");
        registerMessage(1007, "human", "角色服务");
        registerMessage(1008, "human", "角色服务");
        registerMessage(1009, "human", "角色服务");
        registerMessage(1010, "human", "角色服务");
        registerMessage(1110, "human", "角色服务");
        registerMessage(1111, "human", "角色服务");

        // ========== 场景相关 ==========
        registerMessage(1201, "stage", "场景服务");
        registerMessage(1202, "stage", "场景服务");
        registerMessage(1203, "stage", "场景服务");
        registerMessage(1204, "stage", "场景服务");
        registerMessage(1211, "stage", "场景服务");
        registerMessage(1212, "stage", "场景服务");
        registerMessage(1213, "stage", "场景服务");
        registerMessage(1214, "stage", "场景服务");
        registerMessage(1215, "stage", "场景服务");
        registerMessage(1216, "stage", "场景服务");
        registerMessage(1217, "stage", "场景服务");
        registerMessage(1220, "stage", "场景服务");

        // ========== 战斗相关 ==========
        registerMessage(1301, "battle", "战斗服务");

        // ========== 物品相关 ==========
        registerMessage(1401, "item", "物品服务");
        registerMessage(1402, "item", "物品服务");

        // ========== 任务相关 ==========
        registerMessage(1501, "quest", "任务服务");
        registerMessage(1502, "quest", "任务服务");

        // ========== 好友相关 ==========
        registerMessage(1601, "friend", "好友服务");
        registerMessage(1602, "friend", "好友服务");

        // ========== 聊天相关 ==========
        registerMessage(1701, "chat", "聊天服务");
        registerMessage(1702, "chat", "聊天服务");

        logger.info("默认协议映射注册完成，共 {} 条消息", messageServiceMap.size());
    }

    /**
     * 注册消息映射
     */
    public void registerMessage(int msgId, String serviceType, String serviceName) {
        messageServiceMap.put(msgId, serviceType);
        messageNameMap.put(msgId, serviceName);
    }

    /**
     * 获取消息对应的服务类型
     */
    public String getServiceType(int msgId) {
        return messageServiceMap.get(msgId);
    }

    /**
     * 获取消息名称
     */
    public String getMessageName(int msgId) {
        return messageNameMap.get(msgId);
    }

    /**
     * 检查是否为 CS 消息（客户端到服务器）
     */
    public boolean isClientMessage(int msgId) {
        return (msgId >= 100 && msgId < 2000) || (msgId >= 10000 && msgId < 20000);
    }

    /**
     * 检查是否为 SC 消息（服务器到客户端）
     */
    public boolean isServerMessage(int msgId) {
        return (msgId >= 1000 && msgId < 3000) || (msgId >= 11000 && msgId < 30000);
    }

    /**
     * 检查是否为 SS 消息（服务器间通信）
     */
    public boolean isInternalMessage(int msgId) {
        return msgId >= 30000;
    }

    /**
     * 获取所有注册的消息ID
     */
    public int[] getRegisteredMessageIds() {
        return messageServiceMap.keySet().stream()
                .mapToInt(Integer::intValue)
                .toArray();
    }
}
