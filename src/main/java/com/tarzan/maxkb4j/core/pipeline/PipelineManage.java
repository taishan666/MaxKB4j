package com.tarzan.maxkb4j.core.pipeline;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Answer;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domain.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
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
    public Sinks.Many<ChatMessageVO> sink;

    public PipelineManage(List<AbsStep> stepList) {
        this.stepList = stepList;
        this.context = new HashMap<>();
        this.context.put("messageTokens", 0);
        this.context.put("answerTokens", 0);
    }


    public Answer run(ApplicationVO application, ChatParams chatParams, Sinks.Many<ChatMessageVO> sink) {
        if (application != null) {
            this.application= application;
        }
        if (chatParams != null) {
            this.chatParams= chatParams;
        }
        if (sink != null){
            this.sink = sink;
        }
        for (AbsStep step : stepList) {
            step.run(this);
        }
        String answer =(String) this.context.getOrDefault("answer","");
        return Answer.builder().content(answer).reasoningContent("").viewType("many_view").runtimeNodeId("ai-chat-node").build();
    }

    public List<ChatMessage> getHistoryMessages(int dialogueNumber) {
        List<ChatMessage> historyMessages=new ArrayList<>();
        List<ApplicationChatRecordEntity> historyChatRecords= chatParams.getHistoryChatRecords();
        int total=historyChatRecords.size();
        int startIndex = Math.max(total - dialogueNumber, 0);
        for (int i = startIndex; i < total; i++) {
            historyMessages.add(new UserMessage(historyChatRecords.get(i).getProblemText()));
            historyMessages.add(new AiMessage(historyChatRecords.get(i).getAnswerText()));
        }
        return historyMessages;
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

