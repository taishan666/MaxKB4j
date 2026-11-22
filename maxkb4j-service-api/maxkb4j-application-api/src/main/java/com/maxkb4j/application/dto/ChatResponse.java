package com.maxkb4j.application.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.vo.Answer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
        if (answers == null || answers.isEmpty()) {
            return arrays;
        }
        List<Answer> currentGroup = new ArrayList<>();
        String currentViewType = null;
        boolean firstElement = true;
        for (Answer answer : answers) {
            String viewType = answer.getViewType();
            // 首元素直接开组；此后 viewType 变化（含 null 与非 null 互转）则切组
            if (!firstElement && !Objects.equals(currentViewType, viewType)) {
                // viewType 改变，将当前组加入结果，并开启新组
                arrays.add(currentGroup);
                currentGroup = new ArrayList<>();
            }
            currentGroup.add(answer);
            currentViewType = viewType;
            firstElement = false;
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
        if (runDetails == null) {
            return 0;
        }
        return runDetails.values().stream()
                .map(row -> (JSONObject) row)
                .filter(row -> row.containsKey(fieldName) && row.get(fieldName) != null)
                .mapToInt(row -> row.getIntValue(fieldName))
                .sum();
    }
}