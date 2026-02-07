package org.gof.core.rpc.imp;

import java.util.concurrent.TimeUnit;

import org.gof.core.rpc.rpctask.AsyncRpcTask;
import org.gof.core.rpc.rpctask.SyncRpcTask;
import org.gof.core.rpc.listener.ITaskFinishListener;
import org.gof.core.rpc.message.rpcReq.AbstractMessage;

/**
 * 远程实现
 * 
 * @param taskMessageWrap
 * @param time
 * @param timeUnit
 * @return
 */
public interface IRemoteImp {

    public ITaskFinishListener getTaskFinishListener();

    public <T> T get(AbstractMessage msg, SyncRpcTask<T> syncRpcTask, long time, TimeUnit timeUnit);

    public <T> void runAsync(AbstractMessage msg, AsyncRpcTask<T> AsyncRpcTask, long time, TimeUnit timeUnit);
}
