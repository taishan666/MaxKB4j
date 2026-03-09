package com.maxkb4j.application.vo;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.knowledge.vo.ParagraphVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationChatRecordVO extends ApplicationChatRecordEntity {

    private List<ParagraphVO> paragraphList;
    private String paddingProblemText;
    private List<JSONObject> executionDetails;
}
