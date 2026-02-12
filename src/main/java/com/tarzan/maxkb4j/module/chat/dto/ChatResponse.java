package com.tarzan.maxkb4j.module.chat.dto;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.model.Answer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChatResponse {

    private List<Answer> answerTextList;
    private Integer messageTokens;
    private Integer answerTokens;
    private JSONObject runDetails;

    public ChatResponse(List<Answer> answerTextList, JSONObject runDetails) {
        this.answerTextList = answerTextList;
        this.runDetails = runDetails;
    }

    public JSONArray getAnswerJSONArray() {
        JSONArray arrays = new JSONArray();
        List<Answer> currentGroup = new ArrayList<>();
        String currentViewType = null;
        for (Answer answer : answerTextList) {
            String viewType = answer.getViewType();
            // 如果是第一个元素，或者 viewType 与当前组一致，则加入当前组
            if (currentViewType == null || currentViewType.equals(viewType)) {
                currentGroup.add(answer);
                currentViewType = viewType;
            } else {
                // viewType 改变，将当前组加入结果，并开启新组
                arrays.add(currentGroup);
                currentGroup = new ArrayList<>();
                currentGroup.add(answer);
                currentViewType = viewType;
            }
        }
        // 循环结束后，别忘了添加最后一组
        if (!currentGroup.isEmpty()) {
            arrays.add(currentGroup);
        }
        return arrays;
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
        return String.join("\n\n", answerTextList.stream().map(Answer::getContent).toList());
    }
}