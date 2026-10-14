package com.maxkb4j.application.pipeline;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.vo.ResultCallback;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.vo.Answer;
import com.maxkb4j.common.domain.vo.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.common.domain.dto.ChatState;
import com.maxkb4j.common.util.MessageConverter;
import com.maxkb4j.knowledge.vo.ParagraphRagVO;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class PipelineManage {
    public List<AbsStep> stepList;
    public Map<String, Object> context;
    public ApplicationVO application;
    public ChatParams chatParams;
    public ChatState chatState;
    public ResultCallback<ChatMessageVO> callback;
    /**
     * 各步骤在本次对话中的私有上下文（按步骤实例隔离，随请求生命周期销毁），
     * 避免步骤单例的实例字段在并发会话间串扰。
     */
    private final Map<AbsStep, Map<String, Object>> stepContexts = new HashMap<>();

    public PipelineManage(List<AbsStep> stepList) {
        this.stepList = stepList;
        this.context = new HashMap<>();
        this.context.put("messageTokens", 0);
        this.context.put("answerTokens", 0);
    }

    /**
     * 获取指定步骤在本次对话中的上下文。
     */
    public Map<String, Object> stepContext(AbsStep step) {
        return stepContexts.computeIfAbsent(step, k -> new JSONObject());
    }


    public Answer run(ApplicationVO application, ChatParams chatParams, ChatState chatState, ResultCallback<ChatMessageVO> callback) {
        if (application != null) {
            this.application = application;
        }
        if (chatParams != null) {
            this.chatParams = chatParams;
        }
        if (chatState != null) {
            this.chatState = chatState;
        }
        if (callback != null) {
            this.callback = callback;
        }
        for (AbsStep step : stepList) {
            try {
                step.run(this);
            } catch (Exception e) {
                // 不在此处向 sink emit 错误：错误由 chatMessageAsync 的统一出口收尾，
                // 避免与上层异常处理双发；保留原始异常类型，便于全局异常处理器映射 i18n
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new RuntimeException(e);
            }
        }
        String answer = (String) this.context.getOrDefault("answer", "");
        String reasoningContent = (String) this.context.getOrDefault("reasoningContent", "");
        return Answer.builder().content(answer).reasoningContent(reasoningContent).viewType("many_view").runtimeNodeId("ai-chat-node").build();
    }

    public List<ChatMessage> getHistoryMessages(int dialogueNumber) {
        return MessageConverter.toHistoryMessages(chatState.getHistoryChatRecords(), dialogueNumber);
    }

    public JSONArray formatHistoryMessages(List<ChatMessage> historyMessages) {
        return MessageConverter.formatHistoryMessages(historyMessages);
    }

    @SuppressWarnings("unchecked")
    public List<String> getExcludeParagraphIds(String problemText) {
        List<String> excludeParagraphIds = new ArrayList<>();
        // 从 chatState 获取历史记录（类型为 List<ChatRecordDTO>），而非从未填充的 context
        List<ChatRecordDTO> chatRecordList = chatState != null ? chatState.getHistoryChatRecords() : null;
        if (!CollectionUtils.isEmpty(chatRecordList)) {
            for (ChatRecordDTO chatRecord : chatRecordList) {
                JSONObject details = chatRecord.getDetails();
                if (details != null && !details.isEmpty()) {
                    if (problemText.equals(chatRecord.getProblemText()) && details.containsKey("search_step")) {
                        JSONObject searchStep = details.getJSONObject("search_step");
                        // 历史数据可能缺少 paragraphList 键，判空避免 NPE
                        JSONArray paragraphArray = searchStep.getJSONArray("paragraphList");
                        if (paragraphArray != null) {
                            List<ParagraphRagVO> paragraphList = paragraphArray.toJavaList(ParagraphRagVO.class);
                            if (!CollectionUtils.isEmpty(paragraphList)) {
                                excludeParagraphIds.addAll(paragraphList.stream().map(ParagraphRagVO::getId).toList());
                            }
                        }
                    }
                }
            }
        }
        return excludeParagraphIds;
    }


    public JSONObject getDetails() {
        JSONObject details = new JSONObject();
        for (AbsStep row : stepList) {
            JSONObject item = row.getDetails(this);
            if (item != null) {
                String stepType = item.getString("step_type");
                details.put(stepType, item);
            }
        }
        return details;
    }

    public static class Builder {
        private final List<AbsStep> stepList = new ArrayList<>();

        public void addStep(AbsStep step) {
            stepList.add(step);
        }

        public PipelineManage build() {
            return new PipelineManage(stepList);
        }
    }
}

