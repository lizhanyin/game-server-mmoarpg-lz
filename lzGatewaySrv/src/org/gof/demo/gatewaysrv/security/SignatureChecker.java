package org.gof.demo.gatewaysrv.security;

import org.gof.core.Port;
import org.gof.core.Service;
import org.gof.core.gen.proxy.DistrClass;
import org.gof.core.gen.proxy.DistrMethod;
import org.gof.demo.gatewaysrv.support.GatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 签名校验器
 * 验证消息签名的合法性
 */
@DistrClass
public class SignatureChecker extends Service {

    private static final Logger logger = LoggerFactory.getLogger(SignatureChecker.class);

    /** 默认签名密钥 */
    private static final String DEFAULT_SECRET_KEY = "game-server-secret-key-2024";

    /** 当前使用的密钥 */
    private volatile String secretKey;

    /** HMAC算法 */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * 构造函数
     */
    public SignatureChecker(Port port) {
        super(port);
        this.secretKey = DEFAULT_SECRET_KEY;
    }

    @Override
    public Object getId() {
        return "gateway.signature";
    }

    /**
     * 设置签名密钥
     */
    @DistrMethod
    public void setSecretKey(String key) {
        this.secretKey = key;
        logger.info("更新签名密钥");
    }

    /**
     * 生成签名
     */
    @DistrMethod
    public String generateSignature(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            logger.error("生成签名失败", e);
            return null;
        }
    }

    /**
     * 验证签名
     */
    @DistrMethod
    public boolean verifySignature(String data, String signature) {
        if (!GatewayConfig.ENABLE_SIGNATURE_CHECK()) {
            return true;
        }

        String calculated = generateSignature(data);
        if (calculated == null) {
            return false;
        }

        boolean valid = calculated.equals(signature);
        if (!valid) {
            logger.warn("签名验证失败：data={}, expected={}, actual={}",
                    data, calculated, signature);
        }

        return valid;
    }

    /**
     * 验证消息签名（带时间戳）
     */
    @DistrMethod
    public boolean verifyMessageSignature(long humanId, int msgId, byte[] msgData,
                                          long timestamp, String signature) {
        if (!GatewayConfig.ENABLE_SIGNATURE_CHECK()) {
            return true;
        }

        // 1. 检查时间戳（防重放攻击，5分钟有效期）
        long now = System.currentTimeMillis();
        if (Math.abs(now - timestamp) > 5 * 60 * 1000) {
            logger.warn("签名时间戳过期：humanId={}, timestamp={}", humanId, timestamp);
            return false;
        }

        // 2. 构建待签名数据
        String data = buildSignData(humanId, msgId, msgData, timestamp);

        // 3. 验证签名
        return verifySignature(data, signature);
    }

    /**
     * 生成消息签名
     */
    @DistrMethod
    public String generateMessageSignature(long humanId, int msgId, byte[] msgData) {
        long timestamp = System.currentTimeMillis();
        String data = buildSignData(humanId, msgId, msgData, timestamp);
        return generateSignature(data) + ":" + timestamp;
    }

    /**
     * 构建待签名数据
     */
    private String buildSignData(long humanId, int msgId, byte[] msgData, long timestamp) {
        // 数据格式：humanId:msgId:timestamp:dataHash
        String dataHash = hashData(msgData);
        return humanId + ":" + msgId + ":" + timestamp + ":" + dataHash;
    }

    /**
     * 计算数据哈希
     */
    private String hashData(byte[] data) {
        try {
            if (data == null || data.length == 0) {
                return "";
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (Exception e) {
            logger.error("计算数据哈希失败", e);
            return "";
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 验证登录token
     */
    @DistrMethod
    public boolean verifyLoginToken(String token, long humanId, String deviceId) {
        if (!GatewayConfig.ENABLE_SIGNATURE_CHECK()) {
            return true;
        }

        // TODO: 实现token验证逻辑
        // 1. 解析token（通常是JWT）
        // 2. 验证签名
        // 3. 检查过期时间
        // 4. 检查用户ID和设备ID匹配

        return true;
    }

    /**
     * 生成登录token
     */
    @DistrMethod
    public String generateLoginToken(long humanId, String deviceId) {
        // TODO: 实现token生成逻辑（JWT）
        return "token_" + humanId + "_" + System.currentTimeMillis();
    }

    /**
     * 验证设备指纹
     */
    @DistrMethod
    public boolean verifyDeviceFingerprint(String fingerprint, long humanId) {
        // TODO: 实现设备指纹验证逻辑
        // 可以与数据库中的设备指纹进行比对
        return true;
    }
}
