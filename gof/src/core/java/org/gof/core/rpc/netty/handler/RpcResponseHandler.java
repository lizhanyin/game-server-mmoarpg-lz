package org.gof.core.rpc.netty.handler;

import java.util.concurrent.ExecutorService;

import org.gof.core.rpc.listener.ITaskFinishListener;
import org.gof.core.rpc.rpctask.AbstractRpcTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSONObject;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * 
 * @author WinkeyZhao
 * @note
 *
 */
public class RpcResponseHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger logger = LoggerFactory.getLogger(RpcResponseHandler.class);

	private ITaskFinishListener taskFinishListener;
	private ExecutorService taskExecutors;

	public RpcResponseHandler(ExecutorService taskExecutors, ITaskFinishListener taskFinishListener) {
		this.taskFinishListener = taskFinishListener;
		this.taskExecutors = taskExecutors;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
		JSONObject jsonObj = JSONObject.parseObject(s);
		// netty发送和接收是异步的所以 taskId也要带着回来.
		String taskId = jsonObj.getString("taskId");
		// 直接移调rpcTask
		AbstractRpcTask<?> rpcTask = taskFinishListener.getAndRemoveRpcTask(taskId);
		if (rpcTask == null) {
			logger.warn("没有找到RpcTask或者已经超时移除: taskId={}", taskId);
			return;
		}
		Class<?> clazz = rpcTask.getClazz();
		// 赋值
		rpcTask.setReturnData(jsonObj.toJavaObject(clazz));
		// 与netty线程脱离
		taskExecutors.submit(rpcTask);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.warn("RPC 连接断开，清理所有待处理任务");
		// 连接断开时清理所有待处理任务
		taskFinishListener.clear();
	}

	public ITaskFinishListener getTaskFinishListener() {
		return taskFinishListener;
	}
}
