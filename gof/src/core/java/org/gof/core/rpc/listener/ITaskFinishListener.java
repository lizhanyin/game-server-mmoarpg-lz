package org.gof.core.rpc.listener;

import org.gof.core.rpc.message.rpcReq.AbstractMessage;
import org.gof.core.rpc.rpctask.AbstractRpcTask;
import org.gof.core.rpc.rpctask.AsyncRpcTask;
import org.gof.core.rpc.rpctask.SyncRpcTask;

/**
 * 
 * @author WinkeyZhao
 * @note 收到netty回调信息后的监听
 *
 */
public interface ITaskFinishListener {

    public <T> T taskSyncFinish(SyncRpcTask<T> rpcTask);

    public <T> void taskAsyncFinish(AsyncRpcTask<T> asyncRpcTask) throws Exception;

    public void timeOut(AbstractMessage msg);

    public void addRpcTask(AbstractRpcTask<?> rpcTask);

    public AbstractRpcTask<?> getAndRemoveRpcTask(String taskId);

    public void clear();

}
