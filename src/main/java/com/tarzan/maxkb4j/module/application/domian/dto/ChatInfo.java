package com.tarzan.maxkb4j.module.application.domian.dto;

import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.logic.LfEdge;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.util.BeanUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ChatInfo implements Serializable {

    private String chatId;
    private ApplicationVO application;
    private List<String> datasetIds;
    private List<String> excludeDocumentIds;
    private List<ApplicationChatRecordEntity> chatRecordList= new ArrayList<>();
    private List<INode> nodes;
    private List<LfEdge> edges;

    public Map<String, Object> toBasePipelineManageParams(){
        return BeanUtil.toMap(this);
    }

    public Map<String, Object> toPipelineManageParams(String chatRecordId,String problemText,List<String> excludeParagraphIds, String chatUserId,String chatUserType, boolean stream){
        Map<String, Object> params = toBasePipelineManageParams();
        params.put("chatRecordId", chatRecordId);
        params.put("problem_text", problemText);
        params.put("exclude_paragraph_ids", excludeParagraphIds);
        chatUserId=chatUserId==null?"":chatUserId;
        params.put("chat_user_id", chatUserId);
        params.put("chat_user_type", chatUserType);
        params.put("stream", stream);
        return params;
    }

    public void addChatRecord(ApplicationChatRecordEntity chatRecord) {
          this.chatRecordList.add(chatRecord);
    }
}
