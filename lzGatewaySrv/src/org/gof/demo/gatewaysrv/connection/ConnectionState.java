package org.gof.demo.gatewaysrv.connection;

/**
 * 连接状态枚举
 */
public enum ConnectionState {
    /** 已连接（未认证） */
    CONNECTED,

    /** 认证中 */
    AUTHENTICATING,

    /** 已认证（已登录） */
    AUTHENTICATED,

    /** 绑定到游戏服务 */
    BOUND,

    /** 已关闭 */
    CLOSED,

    /** 错误状态 */
    ERROR
}
