package com.tarzan.maxkb4j.core.ragpipeline;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatMessageVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class PipelineManage {
    public List<IChatPipelineStep> stepList;
    public JSONObject context;
    public Sinks.Many<ChatMessageVO> sink;
    public String answer;

    public PipelineManage(List<IChatPipelineStep> stepList) {
        this.stepList = stepList;
        this.context = new JSONObject();
        this.context.put("messageTokens", 0);
        this.context.put("answerTokens", 0);
    }


    private static IChatPipelineStep instantiateStep(Class<? extends IChatPipelineStep> stepClass) {
        try {
           return stepClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public String run(Map<String, Object> context, Sinks.Many<ChatMessageVO> sink) {
        this.context.put("start_time", System.currentTimeMillis());
        if (context != null) {
            this.context.putAll(context);
        }
        if (sink != null){
            this.sink = sink;
        }
        for (IChatPipelineStep step : stepList) {
            step.run(this);
        }
        this.context.put("runTime", System.currentTimeMillis());
        return answer;
    }

    public List<ChatMessage> getHistoryMessages(int dialogueNumber) {
        List<ChatMessage> historyMessages=new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<ApplicationChatRecordEntity> historyChatRecords= (List<ApplicationChatRecordEntity>) context.get("history_chat_records");
        int total=historyChatRecords.size();
        int startIndex = Math.max(total - dialogueNumber, 0);
        for (int i = startIndex; i < total; i++) {
            historyMessages.add(new UserMessage(historyChatRecords.get(i).getProblemText()));
            historyMessages.add(new AiMessage(historyChatRecords.get(i).getAnswerText()));
        }
        return historyMessages;
    }


    public JSONObject getDetails() {
        JSONObject details = new JSONObject();
        for (IChatPipelineStep row : stepList) {
            JSONObject item = row.getDetails();
            if (item != null) {
                String stepType = item.getString("step_type");
                details.put(stepType, item);
            }
        }
        return details;
    }

    public static class Builder {
        private final List<IChatPipelineStep> stepList = new ArrayList<>();

        public void addStep(Class<? extends IChatPipelineStep> step) {
            stepList.add(instantiateStep(step));
        }

        public void addStep(IChatPipelineStep step) {
            stepList.add(step);
        }

        public PipelineManage build() {
            return new PipelineManage(stepList);
        }
    }
}

