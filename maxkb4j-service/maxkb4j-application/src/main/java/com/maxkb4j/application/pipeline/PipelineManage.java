package com.maxkb4j.application.pipeline;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.ChatParams;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.domain.dto.Answer;
import com.maxkb4j.common.domain.dto.ChatMessageVO;
import com.maxkb4j.common.domain.dto.ChatRecordDTO;
import com.maxkb4j.knowledge.vo.ParagraphVO;
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
        List<ChatRecordDTO> historyChatRecords= chatParams.getHistoryChatRecords();
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

