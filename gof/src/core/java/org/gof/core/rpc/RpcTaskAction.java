package org.gof.core.rpc;

import java.util.concurrent.TimeUnit;

import org.gof.core.rpc.message.rpcReq.AbstractMessage;

/**
 * 
 * @author WinkeyZhao
 * @note
 *
 */
public interface RpcTaskAction {
    public <T> T get(AbstractMessage message, long time, TimeUnit timeUnit);

    public <T> void runAsync(AbstractMessage message, int backBindId, long backExcuteId, long time, TimeUnit timeUnit);
}
