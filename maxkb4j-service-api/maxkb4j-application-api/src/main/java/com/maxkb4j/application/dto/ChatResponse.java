package com.maxkb4j.application.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.Answer;
import lombok.Data;

import java.util.ArrayList;
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
        List<Answer> currentGroup = new ArrayList<>();
        String currentViewType = null;
        for (Answer answer : answers) {
            String viewType = answer.getViewType();
            // 如果是第一个元素，或者 viewType 与当前组一致，则加入当前组
            if (currentViewType != null && !currentViewType.equals(viewType)) {
                // viewType 改变，将当前组加入结果，并开启新组
                arrays.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
            currentGroup.add(answer);
            currentViewType = viewType;
        }
        // 循环结束后，别忘了添加最后一组
        if (!currentGroup.isEmpty()) {
            arrays.add(currentGroup);
        }
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