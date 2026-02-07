package org.gof.core.rpc.register;

import org.gof.core.rpc.netty.NettyClient;

/**
 * 
 * @author WinkeyZhao
 * @note
 *
 */
public interface IRpcConnectionRegister {

    public void registerConnection(NettyClient nettyClient);

}
