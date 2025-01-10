package com.tarzan.maxkb4j.module.chatpipeline;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.BaseToResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class PipelineManage {
    private List<IBaseChatPipelineStep> stepList;
    private JSONObject context;
    private BaseToResponse baseToResponse;

    public PipelineManage(List<Class<? extends IBaseChatPipelineStep>> stepList, BaseToResponse baseToResponse) {
        this.stepList = instantiateSteps(stepList);
        this.context = new JSONObject();
        this.baseToResponse = baseToResponse != null ? baseToResponse : new SystemToResponse();
    }

    private List<IBaseChatPipelineStep> instantiateSteps(List<Class<? extends IBaseChatPipelineStep>> stepClasses) {
        List<IBaseChatPipelineStep> instantiatedSteps = new ArrayList<>();
        for (Class<? extends IBaseChatPipelineStep> stepClass : stepClasses) {
            try {
                instantiatedSteps.add(stepClass.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return instantiatedSteps;
    }

    public void run(Map<String, Object> context) {
        this.context.put("start_time", System.currentTimeMillis());
        if (context != null) {
            this.context.putAll(context);
        }
        for (IBaseChatPipelineStep step : stepList) {
            step.run(this);
        }
    }

    public Map<String, Map<String, Object>> getDetails() {
        Map<String, Map<String, Object>> detailsMap = new HashMap<>();
        for (IBaseChatPipelineStep row : stepList) {
            Map<String, Object> item = row.getDetails();
            if (item != null) {
                String stepType = (String) item.get("step_type");
                detailsMap.put(stepType, item);
            }
        }
        return detailsMap;
    }

    public static class Builder {
        private final List<Class<? extends IBaseChatPipelineStep>> stepList = new ArrayList<>();
        private BaseToResponse baseToResponse = new SystemToResponse();

        public void appendStep(Class<? extends IBaseChatPipelineStep> step) {
            stepList.add(step);
        }

        public Builder addBaseToResponse(BaseToResponse baseToResponse) {
            this.baseToResponse = baseToResponse;
            return this;
        }

        public PipelineManage build() {
            return new PipelineManage(stepList, baseToResponse);
        }
    }
}

