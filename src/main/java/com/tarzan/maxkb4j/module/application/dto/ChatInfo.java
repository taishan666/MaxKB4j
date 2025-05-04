package com.tarzan.maxkb4j.module.application.dto;

import com.tarzan.maxkb4j.module.application.ragpipeline.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationWorkFlowVersionEntity;
import com.tarzan.maxkb4j.util.BeanUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ChatInfo implements Serializable {

    private String chatId;
    private ApplicationEntity application;
    private List<String> datasetIds;
    private List<String> excludeDocumentIds;
    private List<ApplicationChatRecordEntity> chatRecordList= new ArrayList<>();
    private ApplicationWorkFlowVersionEntity workFlowVersion;

    public Map<String, Object> toBasePipelineManageParams(){
        return BeanUtil.toMap(this);
    }

    public Map<String, Object> toPipelineManageParams(String problemText,PostResponseHandler postResponseHandler,List<String> excludeParagraphIds, String clientId,String clientType, boolean stream){
        Map<String, Object> params = toBasePipelineManageParams();
        params.put("problem_text", problemText);
        params.put("postResponseHandler", postResponseHandler);
        params.put("exclude_paragraph_ids", excludeParagraphIds);
        String client_id=clientId==null?"":clientId;
        params.put("client_id", client_id);
        params.put("client_type", clientType);
        params.put("stream", stream);
        return params;
    }

    public void addChatRecord(ApplicationChatRecordEntity chatRecord) {
          this.chatRecordList.add(chatRecord);
    }
}
