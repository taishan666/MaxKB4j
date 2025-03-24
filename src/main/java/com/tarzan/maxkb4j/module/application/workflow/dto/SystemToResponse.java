package com.tarzan.maxkb4j.module.application.workflow.dto;

import com.alibaba.fastjson.JSONObject;

import java.util.List;

public class SystemToResponse extends BaseToResponse{
    @Override
    public JSONObject toBlockResponse(String chatId, String chatRecordId, String content, boolean isEnd, int completionTokens, int promptTokens) {
        JSONObject data = new JSONObject();
        data.put("chat_id", chatId);
        data.put("chatRecordId", chatRecordId);
        data.put("operate", true);
        data.put("content", content);
        data.put("node_id", "ai-chat-node");
        data.put("node_type", "ai-chat-node");
        data.put("node_is_end", true);
        data.put("view_type", "many_view");
        data.put("is_end", isEnd);
        JSONObject usage = new JSONObject();
        usage.put("completion_tokens", completionTokens);
        usage.put("prompt_tokens", promptTokens);
        usage.put("total_tokens", (promptTokens + completionTokens));
        data.put("usage", usage);
        return data;
    }

    @Override
    public JSONObject toStreamChunkResponse(String chatId, String chatRecordId, String nodeId, List<String> upNodeIdList, String content, boolean isEnd, int completionTokens, int promptTokens) {
        return toStreamChunkResponse(chatId,chatRecordId,nodeId,upNodeIdList,content,isEnd,completionTokens,promptTokens,null);
    }

    @Override
    public JSONObject toStreamChunkResponse(String chatId, String chatRecordId, String nodeId, List<String> upNodeIdList, String content, boolean isEnd, int completionTokens, int promptTokens,ChunkInfo otherParams) {
        JSONObject result=new JSONObject();
        // 添加各个参数到 JSON 对象中
        result.put("chat_id", chatId);
        result.put("chatRecordId", chatRecordId);
        result.put("node_id", nodeId);
        result.put("up_node_ids", upNodeIdList); // 将 List 转换为 JSONArray
        result.put("content", content);
        result.put("is_end", isEnd);
        result.put("completion_tokens", completionTokens);
        result.put("prompt_tokens", promptTokens);
        if(otherParams!=null){
            result.put("node_type", otherParams.getNodeType());
            result.put("runtimeNodeId", otherParams.getRuntimeNodeId());
            result.put("view_type", otherParams.getViewType());
            result.put("child_node", otherParams.getChildNode());
            result.put("node_is_end", otherParams.getNodeIsEnd());
            result.put("real_node_id", otherParams.getRealNodeId());
        }
        return result;
    }
}
