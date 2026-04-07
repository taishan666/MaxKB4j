package com.maxkb4j.workflow.model.params;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

/**
 * AI 聊天节点参数
 * 从 AiChatNode.NodeParams 提取
 */
@Data
public class AiChatNodeParams {
    private String modelId;
    private String system;
    private String prompt;
    private String dialogueType;
    private int dialogueNumber;
    private Boolean isResult;
    private JSONObject modelParamsSetting;
    private JSONObject modelSetting;
    private Boolean toolOutputEnable;
    private List<String> toolIds;
    private List<String> applicationIds;
    private List<String> imageList;
}