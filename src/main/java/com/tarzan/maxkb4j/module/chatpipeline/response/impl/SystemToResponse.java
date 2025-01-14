package com.tarzan.maxkb4j.module.chatpipeline.response.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.chatpipeline.response.BaseToResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class SystemToResponse extends BaseToResponse {

    public JSONObject toBlockResponse(UUID chatId, UUID chatRecordId,String content, Boolean isEnd, int completion_tokens,
                                int prompt_tokens, JSONObject other_params){
        if(other_params == null){
            other_params = new JSONObject();
        }
        JSONObject result = new JSONObject();
        result.put("chat_id", chatId);
        result.put("chat_record_id", chatRecordId);
        result.put("operate", true);
        result.put("content", content);
        result.put("node_id", "ai-chat-node");
        result.put("node_type", "ai-chat-node");
        result.put("node_is_end", true);
        result.put("view_type", "many_view");
        result.put("is_end", isEnd);
       // result.put("completion_tokens", completion_tokens);
       // result.put("prompt_tokens", prompt_tokens);
      //  result.put("other_params", other_params);
        return result;
    }


    private static String convertToUnicode(String input) {
        StringBuilder unicodeString = new StringBuilder();
        for (char c : input.toCharArray()) {
            String unicodeEscape = String.format("\\u%04x", (int) c);
            unicodeString.append(unicodeEscape);
        }
        return unicodeString.toString();
    }


}
