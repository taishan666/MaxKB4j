package com.tarzan.maxkb4j.module.application.workflow.dto;

import cn.dev33.satoken.util.SaResult;

import java.util.List;

public class SystemToResponse extends BaseToResponse{
    @Override
    public SaResult toBlockResponse(String chatId, String chatRecordId, String content, boolean isEnd, int completionTokens, int promptTokens, int status) {
         return SaResult.ok().setData(content);
    }

    @Override
    public String toStreamChunkResponse(String chatId, String chatRecordId, String nodeId, List<String> upNodeIdList, String content, boolean isEnd, int completionTokens, int promptTokens) {
         return super.formatStreamChunk(content);
    }
}
