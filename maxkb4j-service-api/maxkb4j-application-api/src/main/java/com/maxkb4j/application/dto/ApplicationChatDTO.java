package com.maxkb4j.application.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ApplicationChatDTO {
    private String id;
    private String summary;
    private String applicationId;
    private String chatUserId;
    private String chatUserType;
    private JSONObject asker;
    private JSONObject meta;
    private Integer starNum;
    private Integer trampleNum;
    private Integer chatRecordCount;
    private Integer markSum;
    private Boolean isDeleted;
    private String ipAddress;
    private JSONObject source;
}
