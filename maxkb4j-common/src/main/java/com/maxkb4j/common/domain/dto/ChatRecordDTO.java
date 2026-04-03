package com.maxkb4j.common.domain.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.Date;

@Data
public class ChatRecordDTO {
    private String id;
    private String problemText;
    private String answerText;
    private JSONObject details;
    private JSONArray answerTextList;
    private Integer index;
    private Float runTime;
    private Integer messageTokens;
    private Integer answerTokens;
    private String voteStatus;
    private Date createTime;


    public JSONObject getNodeDetailsByRuntimeNodeId(String runtimeNodeId) {
        return details.getJSONObject(runtimeNodeId);
    }
}
