package com.tarzan.maxkb4j.module.application.chatpipeline;

import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationWorkFlowVersionEntity;
import com.tarzan.maxkb4j.module.application.chatpipeline.handler.PostResponseHandler;
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

    public Map<String, Object> toPipelineManageParams(String problemText,PostResponseHandler postResponseHandler,List<UUID> excludeParagraphIds, String clientId,String clientType, boolean stream){
        Map<String, Object> params = toBasePipelineManageParams();
        params.put("problem_text", problemText);
        params.put("postResponseHandler", postResponseHandler);
        params.put("exclude_paragraph_ids", excludeParagraphIds);
        UUID client_id=clientId==null?null:UUID.fromString(clientId);
        params.put("client_id", client_id);
        params.put("client_type", clientType);
        params.put("stream", stream);
        return params;
    }

}
