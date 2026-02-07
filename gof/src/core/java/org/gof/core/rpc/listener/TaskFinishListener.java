package org.gof.core.rpc.listener;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gof.core.rpc.message.rpcReq.AbstractMessage;
import org.gof.core.rpc.rpctask.AbstractRpcTask;
import org.gof.core.rpc.rpctask.AsyncRpcTask;
import org.gof.core.rpc.rpctask.SyncRpcTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 带容量限制的 LRU 缓存实现
 */
class TaskMap extends LinkedHashMap<String, AbstractRpcTask<?>> {
    private static final Logger logger = LoggerFactory.getLogger(TaskMap.class);
    private static final long serialVersionUID = 1L;
    private final int maxCapacity;

    public TaskMap(int maxCapacity) {
        super(16, 0.75f, true); // access-order, LRU
        this.maxCapacity = maxCapacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, AbstractRpcTask<?>> eldest) {
        boolean removed = size() > maxCapacity;
        if (removed) {
            logger.warn("任务缓存已满，移除最旧的任务: taskId={}", eldest.getKey());
            eldest.getValue().cancel();
        }
        return removed;
    }
}

/**
 *
 * @author WinkeyZhao
 * @note rpc任务收到回调监听
 *
 */
public class TaskFinishListener implements ITaskFinishListener {

    private static final Logger logger = LoggerFactory.getLogger(TaskFinishListener.class);
    private static final int DEFAULT_MAX_CAPACITY = 10000;

    private final Map<String, AbstractRpcTask<?>> rpcTaskMap;
    private final int maxCapacity;

    public TaskFinishListener() {
        this(DEFAULT_MAX_CAPACITY);
    }

    public TaskFinishListener(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        this.rpcTaskMap = Collections.synchronizedMap(new TaskMap(maxCapacity));
    }

    /**
     * 添加RpcTask
     */
    @Override
    public synchronized void addRpcTask(AbstractRpcTask<?> rpcTask) {
        rpcTaskMap.put(rpcTask.getTaskId(), rpcTask);
    }

    /**
     * 同步完成
     */
    @Override
    public <T> T taskSyncFinish(SyncRpcTask<T> rpcTask) {
        logger.debug("任务完成: taskId={}", rpcTask.getTaskId());
        // 否者返回接收到的数据
        return (T) rpcTask.getReturnData();
    }

    /**
     * 异步
     */
    @Override
    public <T> void taskAsyncFinish(AsyncRpcTask<T> asyncRpcTask) throws Exception {
        throw new Exception("此方法只能被异步调用");
    }

    /**
     * 超时
     */
    @Override
    public void timeOut(AbstractMessage msg) {
        String taskId = msg.getTaskId();
        logger.warn("任务超时: taskId={}, msg={}", taskId, msg);
        AbstractRpcTask<?> v = rpcTaskMap.remove(taskId);
        if (v == null) {
            logger.warn("超时任务未找到: taskId={}", taskId);
            return;
        }
        v.cancel();
    }

    @Override
    public AbstractRpcTask<?> getAndRemoveRpcTask(String taskId) {
        return rpcTaskMap.remove(taskId);
    }

    /**
     * 连接断开清理
     */
    @Override
    public void clear() {
        rpcTaskMap.values().forEach(e -> {
            e.cancel();
        });
    }

}
