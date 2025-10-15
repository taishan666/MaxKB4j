package com.tarzan.maxkb4j.module.chat;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ChatResponse {

    private String answer;
    private JSONObject runDetails;

    public ChatResponse(String answer,JSONObject runDetails) {
        this.answer = answer;
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
}