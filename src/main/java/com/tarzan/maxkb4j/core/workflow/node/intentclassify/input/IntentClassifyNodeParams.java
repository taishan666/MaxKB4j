package com.tarzan.maxkb4j.core.workflow.node.intentclassify.input;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class IntentClassifyNodeParams {

    private String modelId;
    private JSONObject modelParamsSetting;
    private List<String> contentList;
    private int dialogueNumber;
    private List<IntentClassifyBranch> branch;
}
