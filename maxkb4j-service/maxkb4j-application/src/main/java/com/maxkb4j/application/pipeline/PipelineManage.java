package com.maxkb4j.application.pipeline;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.dto.*;
import com.maxkb4j.common.util.MessageConverter;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Sinks;

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
    public ChatContext chatContext;
    public Sinks.Many<ChatMessageVO> sink;

    public PipelineManage(List<AbsStep> stepList) {
        this.stepList = stepList;
        this.context = new HashMap<>();
        this.context.put("messageTokens", 0);
        this.context.put("answerTokens", 0);
    }


    public Answer run(ApplicationVO application, ChatParams chatParams, ChatContext chatContext, Sinks.Many<ChatMessageVO> sink)  {
        if (application != null) {
            this.application= application;
        }
        if (chatParams != null) {
            this.chatParams= chatParams;
        }
        if (chatContext != null) {
            this.chatContext= chatContext;
        }
        if (sink != null){
            this.sink = sink;
        }
        for (AbsStep step : stepList) {
            try {
                step.run(this);
            } catch (Exception e) {
                if (sink != null) {
                    sink.tryEmitError(e);
                }
                throw new RuntimeException(e);
            }
        }
        String answer =(String) this.context.getOrDefault("answer","");
        String reasoningContent =(String) this.context.getOrDefault("reasoningContent","");
        return Answer.builder().content(answer).reasoningContent(reasoningContent).viewType("many_view").runtimeNodeId("ai-chat-node").build();
    }
    public List<ChatMessage> getHistoryMessages(int dialogueNumber) {
        return MessageConverter.toHistoryMessages(chatContext.getHistoryChatRecords(), dialogueNumber);
    }
    public JSONArray formatHistoryMessages(List<ChatMessage> historyMessages){
        return MessageConverter.formatHistoryMessages(historyMessages);
    }

    @SuppressWarnings("unchecked")
    public List<String> getExcludeParagraphIds(String problemText) {
        List<String> excludeParagraphIds = new ArrayList<>();
        List<ApplicationChatRecordEntity> chatRecordList= (List<ApplicationChatRecordEntity>) context.get("chatRecordList");
        if (!CollectionUtils.isEmpty(chatRecordList)) {
            for (ApplicationChatRecordEntity chatRecord : chatRecordList) {
                JSONObject details = chatRecord.getDetails();
                if (!details.isEmpty()) {
                    if (problemText.equals(chatRecord.getProblemText()) && details.containsKey("search_step")) {
                        JSONObject searchStep = details.getJSONObject("search_step");
                        List<ParagraphVO> paragraphList = searchStep.getJSONArray("paragraphList").toJavaList(ParagraphVO.class);
                        if (!CollectionUtils.isEmpty(paragraphList)) {
                            excludeParagraphIds.addAll(paragraphList.stream().map(ParagraphVO::getId).toList());
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
            JSONObject item = row.getDetails();
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

