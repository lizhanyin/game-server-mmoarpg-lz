package org.gof.core.rpc.message.rpcReq;

import java.util.UUID;

import org.gof.core.rpc.message.IMessage;

import lombok.Getter;

@Getter
public class AbstractMessage implements IMessage {
    protected final int bindId;
    protected final Class<?> clazz;

    public AbstractMessage(int bindId, Class<?> clazz) {
        this.bindId = bindId;
        this.clazz = clazz;
    }

    @Override
    public String getTaskId() {
        return UUID.randomUUID().toString();
    }
}
