package com.maxkb4j.common.domain.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.util.RenderTags;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
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
            newMessageList.removeLast();
        }

        return newMessageList;
    }

    /**
     * 将历史聊天记录转换为 LLM 历史消息，并取最近 {@code dialogueRounds} 轮（每轮 = user + ai 两条）。
     * <p>跳过含 {@code <form_render>} 的记录（UI 控件不应进入上下文），并清理 {@code <tool_calls_render>} 标签。</p>
     *
     * @param records        历史聊天记录，可为 null
     * @param dialogueRounds 保留的对话轮数；&lt;=0 返回空列表
     * @return 最近 N 轮的 ChatMessage 列表
     */
    public static List<ChatMessage> toHistoryMessages(List<ChatRecordDTO> records, int dialogueRounds) {
        List<ChatMessage> messages = new ArrayList<>();
        if (records == null) {
            return messages;
        }
        for (ChatRecordDTO record : records) {
            String answer = record.getAnswerText();
            if (answer == null) {
                answer = "";
            }
            if (RenderTags.containsFormRender(answer)) {
                continue;
            }
            messages.add(new UserMessage(record.getProblemText()));
            messages.add(new AiMessage(RenderTags.stripToolCallsRender(answer)));
        }
        return lastRounds(messages, dialogueRounds);
    }

    /**
     * 取消息列表最后 N 轮（每轮 2 条）；返回独立副本，调用方可安全修改。
     *
     * @param messages 消息列表，可为 null
     * @param rounds   保留轮数；&lt;=0 返回空列表
     * @return 最近 N 轮的消息副本
     */
    public static List<ChatMessage> lastRounds(List<ChatMessage> messages, int rounds) {
        if (messages == null || messages.isEmpty() || rounds <= 0) {
            return new ArrayList<>();
        }
        int total = messages.size();
        int start = Math.max(total - rounds * 2, 0);
        return new ArrayList<>(messages.subList(start, total));
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
            String nodeName,
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
                nodeName,
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
