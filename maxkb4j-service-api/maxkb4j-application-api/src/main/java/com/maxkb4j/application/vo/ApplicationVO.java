package com.maxkb4j.application.vo;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.KnowledgeDTO;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import com.maxkb4j.common.mp.entity.LlmModelSetting;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ApplicationVO  {
    private String id;

    private String name;

    private String desc;

    private String prologue;

    private Integer dialogueNumber;

    private KnowledgeSetting knowledgeSetting;

    private LlmModelSetting modelSetting;

    private Boolean problemOptimization;

    private String modelId;

    private String userId;

    private String icon;

    private String type;

    private JSONObject workFlow;

    private JSONObject modelParamsSetting;

    private String sttModelId;

    private Boolean sttModelEnable;

    private Boolean sttAutoSend;

    private String ttsModelId;

    private Boolean ttsModelEnable;

    private Boolean ttsAutoplay;

    private String ttsType;

    private JSONObject ttsModelParamsSetting;

    private Boolean isPublish;

    private Date publishTime;


    private String problemOptimizationPrompt;

    /*单位天*/
    private Integer cleanTime;

    private Boolean fileUploadEnable;

    private JSONObject fileUploadSetting;

    private String folderId;

    private List<String> toolIds;

    private List<String> applicationIds;

    private List<String> knowledgeIds;

    private Boolean toolOutputEnable;

    private Boolean longTermEnable;

    private List<KnowledgeDTO> knowledgeList;
    private String nickname;
    private Boolean showSource;
    private Boolean showExec;
    private String language;
    private Date createTime;
    private Date updateTime;
}
