package com.tarzan.maxkb4j.module.application.chatpipeline;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class PipelineManage {
    public List<IBaseChatPipelineStep> stepList;
    public JSONObject context;
    public Flux<ChatMessageVO> response;

    public PipelineManage(List<IBaseChatPipelineStep> stepList) {
        this.stepList = stepList;
        this.context = new JSONObject();
        this.context.put("messageTokens", 0);
        this.context.put("answerTokens", 0);
    }


    private static IBaseChatPipelineStep instantiateStep(Class<? extends IBaseChatPipelineStep> stepClass) {
        try {
           return stepClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public void run(Map<String, Object> context) {
        this.context.put("start_time", System.currentTimeMillis());
        if (context != null) {
            this.context.putAll(context);
        }
        for (IBaseChatPipelineStep step : stepList) {
            step.run(this);
        }
        this.context.put("runTime", System.currentTimeMillis());
    }


    public JSONObject getDetails() {
        JSONObject details = new JSONObject();
        for (IBaseChatPipelineStep row : stepList) {
            JSONObject item = row.getDetails();
            if (item != null) {
                String stepType = item.getString("step_type");
                details.put(stepType, item);
            }
        }
        return details;
    }

    public static class Builder {
        private final List<IBaseChatPipelineStep> stepList = new ArrayList<>();

        public void addStep(Class<? extends IBaseChatPipelineStep> step) {
            stepList.add(instantiateStep(step));
        }

        public void addStep(IBaseChatPipelineStep step) {
            stepList.add(step);
        }

        public PipelineManage build() {
            return new PipelineManage(stepList);
        }
    }
}

