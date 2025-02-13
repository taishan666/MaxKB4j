package com.tarzan.maxkb4j.module.application;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.output.Response;
import lombok.Getter;
import lombok.Setter;

import java.util.Iterator;

public class ChatStream {

    @Getter
    @Setter
    private Iterator<String> iterator;

    // 提供设置回调函数的方法
    // 将 onCompleteConsumer 改为一个回调函数接口
    private OnCompleteCallback completeCallback;

    public void onCompleteCallback(OnCompleteCallback completeCallback){
         this.completeCallback = completeCallback;
    }

    // 定义回调函数接口
    public interface OnCompleteCallback {
        void onComplete(Response<AiMessage> response);
    }

    // 调用回调函数的方法
    public void invokeOnComplete(Response<AiMessage> response) {
        if (completeCallback != null) {
            completeCallback.onComplete(response);
        }
    }

}

