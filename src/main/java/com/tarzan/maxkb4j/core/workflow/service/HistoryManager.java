package com.tarzan.maxkb4j.core.workflow.service;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.enums.DialogueType;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatRecordEntity;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 历史消息管理服务
 * 负责管理工作流的历史聊天记录和节点历史消息
 *
 * @param historyChatRecords -- GETTER --
 *                           获取历史记录列表
 */
@Slf4j
public record HistoryManager(List<ApplicationChatRecordEntity> historyChatRecords) {

    /**
     * 表单渲染标签正则表达式
     */
    private static final Pattern FORM_RENDER_PATTERN = Pattern.compile("<form_render>(.*?)</form_render>", Pattern.DOTALL);

    public HistoryManager(List<ApplicationChatRecordEntity> historyChatRecords) {
        this.historyChatRecords = Objects.requireNonNullElseGet(historyChatRecords, () -> new ArrayList<>(0));
    }

    /**
     * 获取历史消息
     *
     * @param dialogueNumber 对话轮数（每轮包含一个用户消息和一个AI消息）
     * @param dialogueType   对话类型：NODE（节点级别）或全局级别
     * @param runtimeNodeId  运行时节点ID（当 dialogueType 为 NODE 时使用）
     * @return 历史消息列表
     */
    public List<ChatMessage> getHistoryMessages(int dialogueNumber, String dialogueType, String runtimeNodeId) {
        List<ChatMessage> historyMessages;
        if (DialogueType.NODE.name().equals(dialogueType)) {
            historyMessages = getNodeMessages(runtimeNodeId);
        } else {
            historyMessages = getWorkFlowMessages();
        }
        int total = historyMessages.size();
        if (total == 0) {
            return historyMessages;
        }
        // 获取最后 N 轮对话
        int startIndex = Math.max(total - dialogueNumber * 2, 0);
        return historyMessages.subList(startIndex, total);
    }

    /**
     * 获取工作流级别的历史消息
     * 排除包含表单渲染的消息
     *
     * @return 工作流历史消息列表
     */
    private List<ChatMessage> getWorkFlowMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        for (ApplicationChatRecordEntity message : historyChatRecords) {
            String answerText = message.getAnswerText();
            Matcher matcher = FORM_RENDER_PATTERN.matcher(answerText);

            // 跳过包含表单渲染的消息
            if (!matcher.find()) {
                messages.add(new UserMessage(message.getProblemText()));
                messages.add(new AiMessage(answerText));
            }
        }

        return messages;
    }

    /**
     * 获取指定节点的历史消息
     *
     * @param runtimeNodeId 运行时节点ID
     * @return 节点历史消息列表
     */
    private List<ChatMessage> getNodeMessages(String runtimeNodeId) {
        List<ChatMessage> messages = new ArrayList<>();
        for (ApplicationChatRecordEntity record : historyChatRecords) {
            // 获取节点详情
            JSONObject nodeDetails = record.getNodeDetailsByRuntimeNodeId(runtimeNodeId);

            if (nodeDetails != null) {
                messages.add(new UserMessage(nodeDetails.getString("question")));
                messages.add(new AiMessage(nodeDetails.getString("answer")));
            }
        }
        return messages;
    }

}
