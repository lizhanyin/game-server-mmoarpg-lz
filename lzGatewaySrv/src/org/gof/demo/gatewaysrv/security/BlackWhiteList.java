package org.gof.demo.gatewaysrv.security;

import org.gof.core.Port;
import org.gof.core.Service;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.gof.demo.gatewaysrv.support.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 黑白名单管理器
 * 管理IP黑白名单
 */
@DistrClass
public class BlackWhiteList extends Service {

    private static final Logger logger = LoggerFactory.getLogger(BlackWhiteList.class);

    /** 黑名单：IP -> 封禁原因 */
    private final ConcurrentHashMap<String, String> blacklist;

    /** 白名单：IP -> 备注 */
    private final ConcurrentHashMap<String, String> whitelist;

    /** 是否启用白名单模式（白名单模式只允许白名单IP访问） */
    private volatile boolean whitelistMode;

    /**
     * 构造函数
     */
    public BlackWhiteList(Port port) {
        super(port);
        this.blacklist = new ConcurrentHashMap<>();
        this.whitelist = new ConcurrentHashMap<>();
        this.whitelistMode = false;
    }

    @Override
    public Object getId() {
        return "gateway.blackwhitelist";
    }

    /**
     * 检查IP是否被允许访问
     */
    @DistrMethod
    public boolean isAllowed(String ip) {
        if (!GatewayConfig.ENABLE_BLACK_WHITE_LIST()) {
            return true;
        }

        // 检查黑名单
        if (blacklist.containsKey(ip)) {
            logger.warn("IP在黑名单中：{}", ip);
            return false;
        }

        // 白名单模式
        if (whitelistMode) {
            boolean allowed = whitelist.containsKey(ip);
            if (!allowed) {
                logger.warn("IP不在白名单中（白名单模式）：{}", ip);
            }
            return allowed;
        }

        return true;
    }

    /**
     * 添加到黑名单
     */
    @DistrMethod
    public void addBlacklist(String ip, String reason) {
        blacklist.put(ip, reason);
        logger.info("添加黑名单：ip={}, reason={}", ip, reason);

        // 从白名单中移除（如果存在）
        whitelist.remove(ip);
    }

    /**
     * 从黑名单移除
     */
    @DistrMethod
    public void removeBlacklist(String ip) {
        String removed = blacklist.remove(ip);
        if (removed != null) {
            logger.info("移除黑名单：ip={}", ip);
        }
    }

    /**
     * 添加到白名单
     */
    @DistrMethod
    public void addWhitelist(String ip, String remark) {
        whitelist.put(ip, remark);
        logger.info("添加白名单：ip={}, remark={}", ip, remark);

        // 从黑名单中移除（如果存在）
        blacklist.remove(ip);
    }

    /**
     * 从白名单移除
     */
    @DistrMethod
    public void removeWhitelist(String ip) {
        String removed = whitelist.remove(ip);
        if (removed != null) {
            logger.info("移除白名单：ip={}", ip);
        }
    }

    /**
     * 设置白名单模式
     */
    @DistrMethod
    public void setWhitelistMode(boolean enabled) {
        this.whitelistMode = enabled;
        logger.info("设置白名单模式：{}", enabled);
    }

    /**
     * 获取黑名单（用于管理界面）
     */
    @DistrMethod
    public Set<String> getBlacklist() {
        return new HashSet<>(blacklist.keySet());
    }

    /**
     * 获取白名单（用于管理界面）
     */
    @DistrMethod
    public Set<String> getWhitelist() {
        return new HashSet<>(whitelist.keySet());
    }

    /**
     * 清空黑名单
     */
    @DistrMethod
    public void clearBlacklist() {
        int size = blacklist.size();
        blacklist.clear();
        logger.info("清空黑名单：count={}", size);
    }

    /**
     * 清空白名单
     */
    @DistrMethod
    public void clearWhitelist() {
        int size = whitelist.size();
        whitelist.clear();
        logger.info("清空白名单：count={}", size);
    }

    /**
     * 加载配置（从数据库或配置文件）
     */
    @DistrMethod
    public void loadConfig() {
        // TODO: 从数据库或配置文件加载黑白名单配置
        logger.info("加载黑白名单配置");
    }

    /**
     * 保存配置（到数据库或配置文件）
     */
    @DistrMethod
    public void saveConfig() {
        // TODO: 保存黑白名单配置到数据库或配置文件
        logger.info("保存黑白名单配置");
    }
}
