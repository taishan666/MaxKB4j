package com.maxkb4j.common.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

@Data
public class ParagraphDTO {
    private String title;
    private String content;
    private Float similarity;
    private String documentName;
    private JSONObject meta;
    private String knowledgeName;
    private Integer knowledgeType;
}
