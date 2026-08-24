package com.maxkb4j.application.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import lombok.Data;

import java.util.List;

@Data
public class ChatResponse {

    private List<Answer> answers;
    private JSONObject runDetails;

    public ChatResponse(List<Answer> answers, JSONObject runDetails) {
        this.answers = answers;
        this.runDetails = runDetails;
    }

    public JSONArray getAnswerTextList() {
        JSONArray arrays = new JSONArray();
        arrays.addAll(answers);
        return arrays;
    }

    /**
     * 获取消息Token总数
     */
    public Integer getMessageTokens() {
        return sumTokenField("messageTokens");
    }

    /**
     * 获取回答Token总数
     */
    public Integer getAnswerTokens() {
        return sumTokenField("answerTokens");
    }

    public String getAnswer() {
        return String.join("\n\n", answers.stream().map(Answer::getContent).toList());
    }


    /**
     * 通用Token求和方法，消除重复的Stream处理逻辑
     */
    private int sumTokenField(String fieldName) {
        return runDetails.values().stream()
                .map(row -> (JSONObject) row)
                .filter(row -> row.containsKey(fieldName) && row.get(fieldName) != null)
                .mapToInt(row -> row.getIntValue(fieldName))
                .sum();
    }
}