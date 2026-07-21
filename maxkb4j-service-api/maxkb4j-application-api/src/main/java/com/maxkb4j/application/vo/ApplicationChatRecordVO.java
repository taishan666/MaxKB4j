package com.maxkb4j.application.vo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class ApplicationChatRecordVO{
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
    private List<ParagraphRecordVO> paragraphList;
    private String paddingProblemText;
    private List<JSONObject> executionDetails;

    public void setExecutionDetails(List<JSONObject> executionDetails) {
        this.executionDetails = executionDetails;
        this.setDetails(null);
    }
}
