package com.maxkb4j.core.event;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class GenerateProblemEvent extends ApplicationEvent {

    private final String knowledgeId;
    private final List<String> documentIdList;
    private final String modelId;
    private final JSONObject modelParamsSetting;
    private final Integer number;
    private final List<String> stateList;

    public GenerateProblemEvent(Object source, String knowledgeId,List<String> documentIdList, String modelId,JSONObject modelParamsSetting,Integer number, List<String> stateList) {
        super(source);
        this.knowledgeId = knowledgeId;
        this.documentIdList = documentIdList;
        this.modelId = modelId;
        this.modelParamsSetting = modelParamsSetting;
        this.number = number;
        this.stateList = stateList;
    }
}
