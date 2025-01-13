package com.tarzan.maxkb4j.module.chatpipeline;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class SystemToResponse extends BaseToResponse {

    public JSONObject toBlockResponse(UUID chat_id, UUID chat_record_id,String content, Boolean is_end, int completion_tokens,
                                int prompt_tokens, JSONObject other_params){
        if(other_params == null){
            other_params = new JSONObject();
        }
        JSONObject result = new JSONObject();
        result.put("chat_id", chat_id);
        result.put("chat_record_id", chat_record_id);
        result.put("content", content);
        result.put("is_end", is_end);
        result.put("completion_tokens", completion_tokens);
        result.put("prompt_tokens", prompt_tokens);
        result.put("other_params", other_params);
        return result;
    }

}
