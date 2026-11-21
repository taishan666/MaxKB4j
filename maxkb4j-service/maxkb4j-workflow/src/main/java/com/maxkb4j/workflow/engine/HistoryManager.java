package com.maxkb4j.workflow.engine;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.util.MessageConverter;
import com.maxkb4j.workflow.enums.DialogueType;
import com.maxkb4j.workflow.model.ChatRecordSimple;
import dev.langchain4j.data.message.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

/**
 * 历史消息管理服务
 * 负责管理工作流的历史聊天记录和节点历史消息
 *
 * @param historyChatRecords -- GETTER --
 *                           获取历史记录列表
 */
@Slf4j
public record HistoryManager(List<ChatRecordDTO> historyChatRecords) {

    public HistoryManager(List<ChatRecordDTO> historyChatRecords) {
        this.historyChatRecords = Objects.requireNonNullElseGet(historyChatRecords, () -> new ArrayList<>(0));
    }

    public List<ChatRecordSimple> getSimpleMessages() {
        List<ChatRecordSimple> list = new ArrayList<>();
        for (ChatRecordDTO chatRecord : historyChatRecords) {
            ChatRecordSimple record = new ChatRecordSimple();
            record.setQuestion(chatRecord.getProblemText());
            record.setAnswer(chatRecord.getAnswerText());
            list.add(record);
        }
        return list;
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
        if (DialogueType.NODE.name().equals(dialogueType)) {
            return MessageConverter.lastRounds(getNodeMessages(runtimeNodeId), dialogueNumber);
        }
        return MessageConverter.toHistoryMessages(historyChatRecords, dialogueNumber);
    }

    /**
     * 获取指定节点的历史消息
     * 注意：langchain4j 1.x 在构造时校验 UserMessage 的 contents 不能为空集合、AiMessage 的 text 不能为 null，
     * 因此无有效问题或回答为 null 的记录需跳过或兜底，避免直接抛异常
     *
     * @param runtimeNodeId 运行时节点ID
     * @return 节点历史消息列表
     */
    private List<ChatMessage> getNodeMessages(String runtimeNodeId) {
        List<ChatMessage> messages = new ArrayList<>();
        for (ChatRecordDTO record : historyChatRecords) {
            // 获取节点详情
            JSONObject nodeDetails = record.getNodeDetailsByRuntimeNodeId(runtimeNodeId);
            if (nodeDetails == null) {
                continue;
            }
            // question 无有效内容（缺失/多模态项全部无效）时不产生 UserMessage，answer 仍被记录
            UserMessage userMessage = buildQuestionMessage(nodeDetails.get(NodeField.QUESTION));
            if (userMessage != null) {
                messages.add(userMessage);
            }
            messages.add(new AiMessage(Objects.requireNonNullElse(nodeDetails.getString(NodeField.ANSWER), "")));
        }
        return messages;
    }

    /**
     * 将节点 question 字段构造为 UserMessage，构造不出有效消息（如图片 url 缺失、文本为 null）时返回 null
     *
     * @param question 节点 question 字段值（String 或 JSONObject 列表）
     * @return UserMessage，question 无有效内容时返回 null
     */
    private static UserMessage buildQuestionMessage(Object question) {
        if (question instanceof String text) {
            return new UserMessage(text);
        }
        if (question instanceof List<?> list) {
            List<Content> contents = new ArrayList<>();
            for (Object object : list) {
                if (!(object instanceof JSONObject content)) {
                    continue;
                }
                String type = content.getString(ChatField.TYPE);
                if (ChatField.TEXT.equals(type)) {
                    String text = content.getString(ChatField.TEXT);
                    if (text != null) {
                        contents.add(TextContent.from(text));
                    }
                } else if (ChatField.IMAGE_URL.equals(type)) {
                    String url = content.getString(ChatField.URL);
                    if (url != null) {
                        contents.add(ImageContent.from(url));
                    }
                }
            }
            return contents.isEmpty() ? null : new UserMessage(contents);
        }
        return null;
    }

}
