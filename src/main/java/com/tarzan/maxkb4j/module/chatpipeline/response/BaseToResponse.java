package com.tarzan.maxkb4j.module.chatpipeline.response;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.UUID;

@Data
public abstract class BaseToResponse {

    public abstract JSONObject toBlockResponse(UUID chatId, UUID chatRecordId, String content, Boolean isEnd, int completionTokens,
                                       int promptTokens, JSONObject otherParams);


    public String formatStreamChunk(String responseStr){
        return "data: " + responseStr + "\n\n";
    }

}
