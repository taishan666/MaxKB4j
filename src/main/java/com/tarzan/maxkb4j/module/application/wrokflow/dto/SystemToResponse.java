package com.tarzan.maxkb4j.module.application.wrokflow.dto;

import java.util.List;
import java.util.Map;

public class SystemToResponse extends BaseToResponse{
    @Override
    public void toBlockResponse(String chatId, String chatRecordId, String content, boolean isEnd, int completionTokens, int promptTokens, Map<String, Object> otherParams, int status) {

    }

    @Override
    public String toStreamChunkResponse(String chatId, String chatRecordId, String nodeId, List<String> upNodeIdList, String content, boolean isEnd, int completionTokens, int promptTokens) {
         return super.formatStreamChunk(content);
    }
}
