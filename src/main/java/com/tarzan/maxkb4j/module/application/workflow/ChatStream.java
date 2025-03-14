package com.tarzan.maxkb4j.module.application.workflow;

import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.Iterator;

@Setter
public class ChatStream {

    @Getter
    private Iterator<String> iterator;

    //该方法要在 setIterator() 方法之前调用
    // 提供设置回调函数的方法
    private Callback callback;

    // 定义回调函数接口
    public interface Callback {
        void onCallback(ChatResponse result);
    }

    // 调用回调函数的方法
    public void onComplete(ChatResponse response) {
        if (callback != null) {
            callback.onCallback(response);
        }
    }

}

