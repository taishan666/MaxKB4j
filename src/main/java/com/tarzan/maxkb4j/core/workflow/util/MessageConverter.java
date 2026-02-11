package com.tarzan.maxkb4j.core.workflow.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChildNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 消息转换工具类
 * 负责工作流中的各种消息转换操作
 */
public class MessageConverter {

    private MessageConverter() {
        // 工具类，不允许实例化
    }

    /**
     * 重置消息列表
     * 将 ChatMessage 列表转换为 JSONArray 格式，并确保成对出现（用户消息+AI消息）
     *
     * @param historyMessages 历史消息列表
     * @return JSONArray 格式的消息列表
     */
    public static JSONArray resetMessageList(List<ChatMessage> historyMessages) {
        if (CollectionUtils.isEmpty(historyMessages)) {
            return new JSONArray();
        }

        JSONArray newMessageList = new JSONArray();
        for (ChatMessage chatMessage : historyMessages) {
            JSONObject message = new JSONObject();

            if (chatMessage instanceof UserMessage userMessage) {
                message.put("role", "user");
                message.put("content", userMessage.singleText());
                newMessageList.add(message);
            }

            if (chatMessage instanceof AiMessage aiMessage) {
                message.put("role", "ai");
                message.put("content", aiMessage.text());
                newMessageList.add(message);
            }
        }

        // 确保消息成对出现（用户+AI）
        if (newMessageList.size() % 2 != 0) {
            newMessageList.remove(newMessageList.size() - 1);
        }

        return newMessageList;
    }

    /**
     * 将节点数据转换为聊天消息VO
     *
     * @param chatId         聊天ID
     * @param chatRecordId   聊天记录ID
     * @param nodeId         节点ID
     * @param content        消息内容
     * @param reasoningContent 推理内容
     * @param upNodeIdList   上游节点ID列表
     * @param runtimeNodeId  运行时节点ID
     * @param type           节点类型
     * @param viewType       视图类型
     * @param childNode      子节点
     * @param nodeIsEnd      节点是否结束
     * @param isEnd    是否结束
     * @return 聊天消息VO
     */
    public static ChatMessageVO toChatMessageVO(
            String chatId,
            String chatRecordId,
            String nodeId,
            String content,
            String reasoningContent,
            List<String> upNodeIdList,
            String runtimeNodeId,
            String realNodeId,
            String type,
            String viewType,
            ChildNode childNode,
            boolean nodeIsEnd,
            boolean isEnd) {

        return new ChatMessageVO(
                chatId,
                chatRecordId,
                nodeId,
                content,
                reasoningContent,
                upNodeIdList,
                runtimeNodeId,
                realNodeId,
                type,
                viewType,
                childNode,
                nodeIsEnd,
                isEnd);
    }


}
