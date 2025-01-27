package com.tarzan.maxkb4j.module.application.workflow.dto;

import cn.dev33.satoken.util.SaResult;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public class SystemToResponse extends BaseToResponse{
    @Override
    public SaResult toBlockResponse(String chatId, String chatRecordId, String content, boolean isEnd, int completionTokens, int promptTokens, int status) {
         return SaResult.ok().setData(content);
    }

    @Override
    public JSONObject toStreamChunkResponse(String chatId, String chatRecordId, String nodeId, List<String> upNodeIdList, String content, boolean isEnd, int completionTokens, int promptTokens) {
        JSONObject result=new JSONObject();
        // 添加各个参数到 JSON 对象中
        result.put("chat_id", chatId);
        result.put("chat_record_id", chatRecordId);
        result.put("node_id", nodeId);
        result.put("up_node_ids", upNodeIdList); // 将 List 转换为 JSONArray
        result.put("content", content);
        result.put("is_end", isEnd);
        result.put("completion_tokens", completionTokens);
        result.put("prompt_tokens", promptTokens);
        return result;
    }
}
