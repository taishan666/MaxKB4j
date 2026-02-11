package com.tarzan.maxkb4j.module.chat.dto;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.List;

@Data
public class ChatResponse {

    private List<String> answerTextList;
    private Integer messageTokens;
    private Integer answerTokens;
    @JsonIgnore
    private JSONObject runDetails;

    public ChatResponse(List<String> answerTextList, JSONObject runDetails) {
        this.answerTextList = answerTextList;
        this.runDetails = runDetails;
    }

    public Integer getMessageTokens() {
        return runDetails.values().stream()
                .map(row -> (JSONObject) row)
                .filter(row -> row.containsKey("messageTokens") && row.get("messageTokens") != null)
                .mapToInt(row -> row.getIntValue("messageTokens"))
                .sum();
    }

    public Integer getAnswerTokens() {
        return runDetails.values().stream()
                .map(row -> (JSONObject) row)
                .filter(row -> row.containsKey("answerTokens") && row.get("answerTokens") != null)
                .mapToInt(row -> row.getIntValue("answerTokens"))
                .sum();
    }

    public String getAnswer() {
        return String.join("\n\n", answerTextList);
    }
}