package org.gof.core.rpc.listener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.gof.core.rpc.asyncHandler.BaseHandler;
import org.gof.core.rpc.rpctask.AsyncRpcTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 异步rpcTask监听,多了一个消息绑定,所以直接继承同步
 *
 * @author WinkeyZhao
 * @note
 *
 */
public class AsyncTaskFinishLisener extends TaskFinishListener {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskFinishLisener.class);

    private Map<Integer, BaseHandler<?>> asyncHandlerMap = new ConcurrentHashMap<>();

    public void registerAsyncHandlerMap(int msgId, BaseHandler<?> handler) {
        asyncHandlerMap.put(msgId, handler);
    }

    /**
     * 异步完成,这里要寻找处理线程保证顺序
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> void taskAsyncFinish(AsyncRpcTask<T> asyncRpcTask) {
        // 找到绑定回调处理的hanlder
        BaseHandler<T> handler = (BaseHandler<T>) asyncHandlerMap.get(asyncRpcTask.getBackBindId());
        if (handler == null) {
            logger.warn("未找到异步处理器: backBindId={}", asyncRpcTask.getBackBindId());
            return;
        }
        // 找到执行者id标识(比如玩家id,线程id去找对应的执行器)
        long backExcuteId = asyncRpcTask.getBackExcuteId();
        // 找到执行器执行以下代码
        handler.action(asyncRpcTask.getReturnData());
    }
}
