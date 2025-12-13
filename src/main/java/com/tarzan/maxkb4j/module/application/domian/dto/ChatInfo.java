package com.tarzan.maxkb4j.module.application.domian.dto;

import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ChatInfo implements Serializable {

    private String chatId;
    private String appId;
    private Map<String, Object>  chatVariables=new HashMap<>(10);
    private List<ApplicationChatRecordEntity> chatRecordList= new ArrayList<>();

/*    public Map<String, Object> toBasePipelineManageParams(){
        return BeanUtil.toMap(this);
    }

    public Map<String, Object> toPipelineManageParams(ApplicationVO application,String chatRecordId, String problemText,boolean reChat, String chatUserId, String chatUserType){
        Map<String, Object> params = toBasePipelineManageParams();
        params.put("application", application);
        params.put("chatRecordId", chatRecordId);
        params.put("problemText", problemText);
        params.put("reChat", reChat);
        params.put("chatUserId", chatUserId);
        params.put("chatUserType", chatUserType);
        return params;
    }*/

    public void addChatRecord(ApplicationChatRecordEntity chatRecord) {
          this.chatRecordList.add(chatRecord);
    }
}
