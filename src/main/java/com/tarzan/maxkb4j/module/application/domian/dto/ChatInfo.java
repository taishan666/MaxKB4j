package com.tarzan.maxkb4j.module.application.domian.dto;

import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.common.util.BeanUtil;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ChatInfo implements Serializable {

    private String chatId;
   // private ApplicationVO application;
    private String appId;
    private String appType;
    private List<ApplicationChatRecordEntity> chatRecordList= new ArrayList<>();
    private boolean debug;
   // private List<INode> nodes;
    //private List<LfEdge> edges;

    public Map<String, Object> toBasePipelineManageParams(){
        return BeanUtil.toMap(this);
    }

    public Map<String, Object> toPipelineManageParams(ApplicationVO application,String chatRecordId, String problemText, List<ApplicationChatRecordEntity> historyChatRecords,List<String> excludeParagraphIds, String chatUserId, String chatUserType, boolean stream){
        Map<String, Object> params = toBasePipelineManageParams();
        params.put("application", application);
        params.put("chatRecordId", chatRecordId);
        params.put("problem_text", problemText);
        params.put("exclude_paragraph_ids", excludeParagraphIds);
        params.put("history_chat_records", historyChatRecords);
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
