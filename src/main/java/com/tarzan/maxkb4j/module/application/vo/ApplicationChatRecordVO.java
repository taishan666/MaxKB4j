package com.tarzan.maxkb4j.module.application.vo;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationChatRecordVO extends ApplicationChatRecordEntity {

    @JsonProperty("paragraph_list")
    private List<ParagraphVO> paragraphList;
    @JsonProperty("padding_problem_text")
    private String paddingProblemText;
    @JsonProperty("execution_details")
    private List<JSONObject> executionDetails;
}
