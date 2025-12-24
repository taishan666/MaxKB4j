package com.tarzan.maxkb4j.core.workflow.model;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class Document {
    private JSONObject meta;
    private String name;
    private List<Paragraph> paragraphs;
    private String knowledgeId;
}
