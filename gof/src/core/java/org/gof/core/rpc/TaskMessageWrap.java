package org.gof.core.rpc;

import com.alibaba.fastjson2.JSON;
import lombok.Getter;

import org.gof.core.rpc.message.IMessage;

@Getter
public class TaskMessageWrap {

    private String taskId;

    private IMessage message;

    public TaskMessageWrap(String taskId, IMessage message) {
        this.taskId = taskId;
        this.message = message;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
