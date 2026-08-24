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
     *
     * @param runtimeNodeId 运行时节点ID
     * @return 节点历史消息列表
     */
    private List<ChatMessage> getNodeMessages(String runtimeNodeId) {
        List<ChatMessage> messages = new ArrayList<>();
        for (ChatRecordDTO record : historyChatRecords) {
            // 获取节点详情
            JSONObject nodeDetails = record.getNodeDetailsByRuntimeNodeId(runtimeNodeId);
            if (nodeDetails != null) {
                Object question = nodeDetails.get(NodeField.QUESTION);
                List<Content> contents = new ArrayList<>();
                if (question instanceof List<?> list){
                    for (Object object : list) {
                        JSONObject content = (JSONObject) object;
                        String type = content.getString(ChatField.TYPE);
                        if (ChatField.TEXT.equals(type)) {
                            contents.add(TextContent.from(content.getString(ChatField.TEXT)));
                        }else if (ChatField.IMAGE_URL.equals(type)) {
                            String url = content.getString(ChatField.URL);
                            if (url!=null) {
                                contents.add(ImageContent.from(content.getString(ChatField.URL)));
                            }
                        }
                    }
                    messages.add(new UserMessage(contents));
                }else if (question instanceof String){
                    contents.add(TextContent.from(String.valueOf(question)));
                    messages.add(new UserMessage(contents));
                }
                messages.add(new AiMessage(nodeDetails.getString(NodeField.ANSWER)));
            }
        }
        return messages;
    }

}
