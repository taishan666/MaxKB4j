package com.maxkb4j.system.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ChatUserStatDTO {

    private String chatUserId;
    private String chatUsertype;
    private JSONObject asker;
    private Integer totalTokens;
    private Integer chatRecordCount;
}
