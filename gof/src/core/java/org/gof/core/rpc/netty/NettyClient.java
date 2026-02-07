package org.gof.core.rpc.netty;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.gof.core.rpc.imp.IRemoteImp;
import org.gof.core.rpc.listener.ITaskFinishListener;
import org.gof.core.rpc.message.rpcReq.AbstractMessage;
import org.gof.core.rpc.netty.handler.RpcResponseHandler;
import org.gof.core.rpc.rpctask.AsyncRpcTask;
import org.gof.core.rpc.rpctask.SyncRpcTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson2.JSON;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * rpc远程客户端netty实现
 * 
 * @author WinkeyZhao
 * @note
 *
 */
public class NettyClient implements IRemoteImp {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

	private Channel channel;
	private EventLoopGroup workerGroup;

	/**
	 * 创建nettyClient
	 *
	 * @param name
	 * @param host
	 * @param port
	 */
	public NettyClient(String name, String host, int port, RpcResponseHandler rpcResponseHandler) {
        this.workerGroup = new MultiThreadIoEventLoopGroup(0, NioIoHandler.newFactory());
		Bootstrap b = new Bootstrap(); // (1)
		b.group(workerGroup); // (2)
		b.channel(NioSocketChannel.class); // (3)
		b.option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.SO_REUSEADDR, true); // (4)
		b.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new StringDecoder());
				ch.pipeline().addLast(new StringEncoder());
				// 回调rpc handler 绑定了对应的rpctask 所有这里不能sharable
				ch.pipeline().addLast("rpcResponseHandler", rpcResponseHandler);
			}
		});
		try {
			ChannelFuture f = b.connect(host, port).sync();
			channel = f.channel();
			logger.info("RPC 客户端连接成功: host={}, port={}", host, port);
		} catch (InterruptedException e) {
			logger.error("RPC 客户端连接失败: host={}, port={}", host, port, e);
			Thread.currentThread().interrupt();
		}
	}

	public ITaskFinishListener getTaskFinishListener() {
		RpcResponseHandler rpcResponseHandler = (RpcResponseHandler) channel.pipeline().get("rpcResponseHandler");
		return rpcResponseHandler.getTaskFinishListener();
	}

	/**
	 * 
	 */
	@Override
	public <T> T get(AbstractMessage msg, SyncRpcTask<T> syncRpcTask, long time, TimeUnit timeUnit) {
		ChannelFuture channelFuture;
		try {
			channelFuture = channel.writeAndFlush(JSON.toJSONString(msg)).sync();
			// 确认消息发送,才阻塞等待
			if (channelFuture.isSuccess()) {
				getTaskFinishListener().addRpcTask(syncRpcTask);
				return syncRpcTask.getFuture().get(time, timeUnit);
			} else {
				logger.error("RPC 发送消息失败: msg={}", msg);
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			logger.error("RPC 调用异常: msg={}", msg, e1);
			getTaskFinishListener().timeOut(msg);
		}
		return null;
	}

	@Override
	public <T> void runAsync(AbstractMessage msg, AsyncRpcTask<T> AsyncRpcTask, long time, TimeUnit timeUnit) {
		channel.writeAndFlush(JSON.toJSONString(msg)).addListener(new GenericFutureListener<Future<? super Void>>() {
			@Override
			public void operationComplete(Future<? super Void> future) throws Exception {
				if (future.isSuccess()) {
					getTaskFinishListener().addRpcTask(AsyncRpcTask);
					CompletableFuture.delayedExecutor(time, timeUnit).execute(() -> {
						// 超时
						getTaskFinishListener().timeOut(msg);
					});
				} else {
					logger.error("RPC 异步发送消息失败: msg={}", msg);
				}
			}
		});
	}

	/**
	 * 关闭客户端，释放资源
	 */
	public void shutdown() {
		try {
			if (channel != null && channel.isOpen()) {
				channel.close().sync();
			}
		} catch (InterruptedException e) {
			logger.error("关闭 channel 时被中断", e);
			Thread.currentThread().interrupt();
		} finally {
			if (workerGroup != null) {
				workerGroup.shutdownGracefully();
				logger.info("RPC 客户端已关闭");
			}
		}
	}
}
