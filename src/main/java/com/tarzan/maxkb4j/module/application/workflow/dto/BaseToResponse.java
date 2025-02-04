package com.tarzan.maxkb4j.module.application.workflow.dto;

import cn.dev33.satoken.util.SaResult;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public abstract class BaseToResponse {

    /**
     * 将当前对象转换为块响应。
     *
     * @param chatId           聊天ID
     * @param chatRecordId     聊天记录ID
     * @param content          响应内容
     * @param isEnd            是否结束
     * @param completionTokens 完成的token数
     * @param promptTokens     提示的token数
     * @param status           HTTP状态码
     */
    public abstract SaResult toBlockResponse(String chatId, String chatRecordId, String content, boolean isEnd,
                                             int completionTokens, int promptTokens,
                                             int status);

    /**
     * 将当前对象转换为流式块响应。
     *
     * @param chatId           聊天ID
     * @param chatRecordId     聊天记录ID
     * @param nodeId           节点ID
     * @param upNodeIdList     上级节点ID列表
     * @param content          响应内容
     * @param isEnd            是否结束
     * @param completionTokens 完成的token数
     * @param promptTokens     提示的token数
     * @param otherParams      其他参数
     */
    public abstract JSONObject toStreamChunkResponse(String chatId, String chatRecordId, String nodeId,
                                                     List<String> upNodeIdList, String content, boolean isEnd,
                                                     int completionTokens, int promptTokens);

    public abstract JSONObject toStreamChunkResponse(String chatId, String chatRecordId, String nodeId,
                                                     List<String> upNodeIdList, String content, boolean isEnd,
                                                     int completionTokens, int promptTokens,ChunkInfo chunkInfo);

    /**
     * 格式化流式块响应。
     *
     * @param responseStr 响应字符串
     * @return 格式化后的响应字符串
     */
    public  String formatStreamChunk(String responseStr) {
        return "data: " + responseStr + "\n\n";
    }
}