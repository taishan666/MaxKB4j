package com.tarzan.maxkb4j.module.knowledge.domain.dto;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
public class DocumentSimple {
    private String name;
    private String content;
    private JSONObject meta;
    private List<ParagraphSimple> paragraphs;
    private String sourceFileId;

    public DocumentSimple(String name, String content) {
        this.name = name;
        this.content = content;
        this.meta = new JSONObject();
        this.paragraphs = new ArrayList<>();
    }

    public DocumentSimple(String name, String content,JSONObject meta) {
        this.name = name;
        this.content = content;
        this.meta = meta;
        this.paragraphs = new ArrayList<>();
    }

    public DocumentSimple(String name, String content,String sourceFileId) {
        this.name = name;
        this.content = content;
        this.sourceFileId = sourceFileId;
        this.paragraphs = new ArrayList<>();
    }
}
