package com.tarzan.maxkb4j.module.application.domain.vo;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.vo.ParagraphVO;
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
