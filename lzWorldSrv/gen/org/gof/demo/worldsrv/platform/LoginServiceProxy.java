package org.gof.demo.worldsrv.platform;

import org.gof.core.Port;
import org.gof.core.CallPoint;
import org.gof.core.Service;
import org.gof.core.support.Distr;
import org.gof.core.support.Param;
import org.gof.core.support.log.LogCore;
import org.gof.core.gen.proxy.ProxyBase;
import org.gof.core.support.function.*;
import org.gof.core.gen.GofGenFile;

/**
 * 登录服务代理
 * 用于远程调用登录验证服务
 */
@GofGenFile
public final class LoginServiceProxy extends ProxyBase {
    public final class EnumCall {
        public static final int ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_CHECK_STRING_STRING = 1;
        public static final int ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_CHECKPASSWORD_STRING_STRING = 2;
        public static final int ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_VALIDATETOKEN_STRING = 3;
        public static final int ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_KICK_STRING_STRING = 4;
    }

    private static final String SERV_ID = "login";

    private CallPoint remote;
    private Port localPort;

    /**
     * 私有构造函数
     * 防止实例被私自创建 必须通过newInstance函数
     */
    private LoginServiceProxy() {
    }

    /**
     * 获取函数指针
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object getMethodFunction(Service service, int methodKey) {
        LoginService serv = (LoginService) service;
        switch (methodKey) {
            case EnumCall.ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_CHECK_STRING_STRING: {
                return (GofFunction2<String, String>) serv::check;
            }
            case EnumCall.ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_CHECKPASSWORD_STRING_STRING: {
                return (GofFunction2<String, String>) serv::checkPassword;
            }
            case EnumCall.ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_VALIDATETOKEN_STRING: {
                return (GofFunction1<String>) serv::validateToken;
            }
            case EnumCall.ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_KICK_STRING_STRING: {
                return (GofFunction2<String, String>) serv::kick;
            }
            default:
                break;
        }
        return null;
    }

    /**
     * 获取实例（默认方式）
     * 通过分布式配置查找服务
     *
     * @return 代理实例
     */
    public static LoginServiceProxy newInstance() {
        String portId = Distr.getPortId(SERV_ID);
        if (portId == null) {
            LogCore.remote.error("通过servId未能找到查找上级Port: servId={}", SERV_ID);
            return null;
        }

        String nodeId = Distr.getNodeId(portId);
        if (nodeId == null) {
            LogCore.remote.error("通过portId未能找到查找上级Node: portId={}", portId);
            return null;
        }

        return createInstance(nodeId, portId, SERV_ID);
    }

    /**
     * 创建实例（指定节点和端口）
     * 用于跨节点调用
     *
     * @param node 节点ID
     * @param port 端口ID
     * @param id   服务ID
     * @return 代理实例
     */
    public static LoginServiceProxy newInstance(String node, String port, Object id) {
        return createInstance(node, port, id);
    }

    /**
     * 创建实例
     *
     * @param node 节点ID
     * @param port 端口ID
     * @param id   服务ID
     * @return 代理实例
     */
    private static LoginServiceProxy createInstance(String node, String port, Object id) {
        LoginServiceProxy inst = new LoginServiceProxy();
        inst.localPort = Port.getCurrent();
        inst.remote = new CallPoint(node, port, id);

        return inst;
    }

    /**
     * 监听返回值（带上下文数组）
     *
     * @param method  回调方法
     * @param context 上下文参数
     */
    public void listenResult(GofFunction2<Param, Param> method, Object... context) {
        listenResult(method, new Param(context));
    }

    /**
     * 监听返回值（带上下文对象）
     *
     * @param method  回调方法
     * @param context 上下文参数
     */
    public void listenResult(GofFunction2<Param, Param> method, Param context) {
        localPort.listenResult(method, context);
    }

    /**
     * 监听返回值（带布尔状态和上下文数组）
     *
     * @param method  回调方法
     * @param context 上下文参数
     */
    public void listenResult(GofFunction3<Boolean, Param, Param> method, Object... context) {
        listenResult(method, new Param(context));
    }

    /**
     * 监听返回值（带布尔状态和上下文对象）
     *
     * @param method  回调方法
     * @param context 上下文参数
     */
    public void listenResult(GofFunction3<Boolean, Param, Param> method, Param context) {
        localPort.listenResult(method, context);
    }

    /**
     * 等待返回值（同步阻塞）
     *
     * @return 返回结果
     */
    public Param waitForResult() {
        return localPort.waitForResult();
    }

    /**
     * 验证账号和token
     *
     * @param account 账号
     * @param token   验证令牌
     */
    public void check(String account, String token) {
        localPort.call(remote, EnumCall.ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_CHECK_STRING_STRING,
                new Object[]{account, token});
    }

    /**
     * 验证账号密码
     *
     * @param account  账号
     * @param password 密码
     */
    public void checkPassword(String account, String password) {
        localPort.call(remote, EnumCall.ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_CHECKPASSWORD_STRING_STRING,
                new Object[]{account, password});
    }

    /**
     * 验证token有效性
     *
     * @param token 验证令牌
     */
    public void validateToken(String token) {
        localPort.call(remote, EnumCall.ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_VALIDATETOKEN_STRING,
                new Object[]{token});
    }

    /**
     * 踢出玩家
     *
     * @param account 账号
     * @param reason  踢出原因
     */
    public void kick(String account, String reason) {
        localPort.call(remote, EnumCall.ORG_GOF_DEMO_WORLDSRV_PLATFORM_LOGINSERVICE_KICK_STRING_STRING,
                new Object[]{account, reason});
    }
}
