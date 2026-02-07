package org.gof.core.rpc.rpcserver;

import com.alibaba.fastjson2.JSONObject;
import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * @author WinkeyZhao
 * @note
 *
 */
public interface RpcActon {

    public void action(ChannelHandlerContext channelHandlerContext, JSONObject jsonObject);
}
