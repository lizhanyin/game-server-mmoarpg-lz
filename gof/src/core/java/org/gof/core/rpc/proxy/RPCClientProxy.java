package org.gof.core.rpc.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import org.gof.core.rpc.RpcClient;
import org.gof.core.rpc.message.rpcReq.AbstractMessage;

/**
 * 
 * @author WinkeyZhao
 * @note
 *
 */
public class RPCClientProxy implements InvocationHandler {

    private AbstractMessage msg;
    private long time;
    private TimeUnit timeUnit;

    public RPCClientProxy(AbstractMessage msg, long time, TimeUnit timeUnit) {
        super();
        this.msg = msg;
        this.time = time;
        this.timeUnit = timeUnit;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return RpcClient.getInstance().get(msg, time, timeUnit);
    }

    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        T val = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] { clazz }, this);
        return val;
    }
}
