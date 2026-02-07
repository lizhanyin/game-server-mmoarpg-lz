package org.gof.demo.gatewaysrv.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 网关配置加载器
 * 从外部配置文件加载配置
 */
public class GatewayConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(GatewayConfigLoader.class);

    /** 配置文件路径 */
    private static final String CONFIG_FILE = "config/gateway.properties";

    /** 配置属性 */
    private final Properties properties;

    public GatewayConfigLoader() {
        this.properties = new Properties();
        loadConfig();
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            properties.load(input);
            logger.info("配置文件加载成功：{}", CONFIG_FILE);
        } catch (IOException e) {
            logger.warn("配置文件加载失败，使用默认配置：{}", CONFIG_FILE);
            // 使用默认配置
            loadDefaultConfig();
        }
    }

    /**
     * 加载默认配置
     */
    private void loadDefaultConfig() {
        GatewayConfig config = GatewayConfig.getInstance();

        // 网络端口配置
        properties.setProperty("gateway.tcp.port", String.valueOf(config.getTcpPort()));
        properties.setProperty("gateway.websocket.port", String.valueOf(config.getWebSocketPort()));
        properties.setProperty("gateway.http.port", String.valueOf(config.getHttpPort()));

        // 连接配置
        properties.setProperty("gateway.max.connections", String.valueOf(config.getMaxConnections()));
        properties.setProperty("gateway.max.requests.per.second", String.valueOf(config.getMaxRequestsPerSecond()));

        // 安全配置
        properties.setProperty("gateway.security.blackwhitelist.enable", String.valueOf(config.isEnableBlackWhiteList()));
        properties.setProperty("gateway.security.ratelimiter.enable", String.valueOf(config.isEnableRateLimiter()));
        properties.setProperty("gateway.security.signature.enable", String.valueOf(config.isEnableSignatureCheck()));
        properties.setProperty("gateway.security.anticheat.enable", String.valueOf(config.isEnableAntiCheat()));

        // 跨服配置
        properties.setProperty("gateway.crossserver.match.enable", String.valueOf(config.isEnableCrossServerMatch()));
        properties.setProperty("gateway.crossserver.team.enable", String.valueOf(config.isEnableCrossServerTeam()));
        properties.setProperty("gateway.match.timeout", String.valueOf(config.getMatchTimeout()));
        properties.setProperty("gateway.team.maxsize", String.valueOf(config.getTeamMaxSize()));
    }

    /**
     * 获取整型配置
     */
    public int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("配置项格式错误：key={}, value={}", key, value);
            return defaultValue;
        }
    }

    /**
     * 获取长整型配置
     */
    public long getLong(String key, long defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.warn("配置项格式错误：key={}, value={}", key, value);
            return defaultValue;
        }
    }

    /**
     * 获取字符串配置
     */
    public String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /**
     * 获取布尔型配置
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * 应用配置到 GatewayConfig
     */
    public void applyConfig() {
        GatewayConfig config = GatewayConfig.getInstance();

        // 网络端口
        config.setTcpPort(getInt("gateway.tcp.port", config.getTcpPort()));
        config.setWebSocketPort(getInt("gateway.websocket.port", config.getWebSocketPort()));
        config.setHttpPort(getInt("gateway.http.port", config.getHttpPort()));

        // 连接配置
        config.setMaxConnections(getInt("gateway.max.connections", config.getMaxConnections()));
        config.setMaxRequestsPerSecond(getInt("gateway.max.requests.per.second", config.getMaxRequestsPerSecond()));

        // 安全配置
        config.setEnableBlackWhiteList(getBoolean("gateway.security.blackwhitelist.enable", config.isEnableBlackWhiteList()));
        config.setEnableRateLimiter(getBoolean("gateway.security.ratelimiter.enable", config.isEnableRateLimiter()));
        config.setEnableSignatureCheck(getBoolean("gateway.security.signature.enable", config.isEnableSignatureCheck()));
        config.setEnableAntiCheat(getBoolean("gateway.security.anticheat.enable", config.isEnableAntiCheat()));

        // 跨服配置
        config.setEnableCrossServerMatch(getBoolean("gateway.crossserver.match.enable", config.isEnableCrossServerMatch()));
        config.setEnableCrossServerTeam(getBoolean("gateway.crossserver.team.enable", config.isEnableCrossServerTeam()));
        config.setMatchTimeout(getInt("gateway.match.timeout", config.getMatchTimeout()));
        config.setTeamMaxSize(getInt("gateway.team.maxsize", config.getTeamMaxSize()));

        logger.info("配置应用完成");
    }

    /**
     * 获取所有配置
     */
    public Properties getProperties() {
        return properties;
    }
}
