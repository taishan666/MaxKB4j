package com.maxkb4j.application.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationChatRecordDTO {
    private String id;
    private String voteStatus;
    private String voteReason;
    private String voteOtherContent;
    private String problemText;
    private String answerText;
    private Integer messageTokens;
    private Integer answerTokens;
    private Integer cost;
    private JSONObject details;
    private List<String> improveParagraphIdList;
    private Float runTime;
    private Integer index;
    private String chatId;
    private JSONArray answerTextList;
}
