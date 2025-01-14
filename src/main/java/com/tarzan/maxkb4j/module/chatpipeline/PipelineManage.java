package com.tarzan.maxkb4j.module.chatpipeline;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.chatpipeline.response.BaseToResponse;
import com.tarzan.maxkb4j.module.chatpipeline.response.impl.SystemToResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class PipelineManage {
    public List<IBaseChatPipelineStep> stepList;
    public JSONObject context;
    public BaseToResponse baseToResponse;

    public PipelineManage(List<IBaseChatPipelineStep> stepList, BaseToResponse baseToResponse) {
        this.stepList = stepList;
        this.context = new JSONObject();
        this.baseToResponse = baseToResponse != null ? baseToResponse : new SystemToResponse();
    }


    private static IBaseChatPipelineStep instantiateStep(Class<? extends IBaseChatPipelineStep> stepClass) {
        try {
           return stepClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    public Object run(Map<String, Object> context) {
        this.context.put("start_time", System.currentTimeMillis());
        if (context != null) {
            this.context.putAll(context);
        }
        for (int i = 0; i < stepList.size(); i++) {
            IBaseChatPipelineStep step=stepList.get(i);
            if(i==stepList.size()-1){
                return  step.run(this);
            }else {
                step.run(this);
            }
        }

       /* for (IBaseChatPipelineStep step : stepList) {
            step.run(this);
        }*/
       // this.context.put("run_time", System.currentTimeMillis());
        return null;
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
        private BaseToResponse baseToResponse = new SystemToResponse();

        public void appendStep(Class<? extends IBaseChatPipelineStep> step) {
            stepList.add(instantiateStep(step));
        }

        public void addStep(IBaseChatPipelineStep step) {
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

