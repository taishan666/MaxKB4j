package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class DocumentSimple {
    private String name;
    private JSONObject meta;
    private List<ParagraphSimple> paragraphs;
    private String sourceFileId;
}
