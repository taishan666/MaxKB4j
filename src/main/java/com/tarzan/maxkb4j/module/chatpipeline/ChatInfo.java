package com.tarzan.maxkb4j.module.chatpipeline;

import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationWorkFlowVersionEntity;
import com.tarzan.maxkb4j.util.BeanUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
public class ChatInfo {

    private UUID chatId;
    private ApplicationEntity application;
    private List<UUID> datasetIds;
    private List<UUID> excludeDocumentIds;
    private List<ApplicationChatRecordEntity> chatRecordList= new ArrayList<>();
    private ApplicationWorkFlowVersionEntity workFlowVersion;

    public Map<String, Object> toBasePipelineManageParams(){
        return BeanUtil.toMap(this);
    }

    public Map<String, Object> toPipelineManageParams(PostResponseHandler postResponseHandler){
        Map<String, Object> params = toBasePipelineManageParams();
        params.put("postResponseHandler", postResponseHandler);
        return params;
    }

    public Map<String, Object> toPipelineManageParams(String problemText,PostResponseHandler postResponseHandler,List<UUID> excludeParagraphIdList, String clientId,String clientType, boolean stream){
        Map<String, Object> params = toBasePipelineManageParams();
        params.put("problemText", problemText);
        params.put("excludeParagraphIdList", excludeParagraphIdList);
        params.put("clientId", clientId);
        params.put("clientType", clientType);
        params.put("stream", stream);
        return params;
    }
}
