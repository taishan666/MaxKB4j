package com.maxkb4j.knowledge.dto;


import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.Data;

@Data
public class ParagraphDTO {
    private String id;
    private String title;
    private String content;
    private String status;
    private Integer hitNum;
    private Boolean isActive;
    private String knowledgeId;
    private String documentId;
    private Integer position;
    private JSONObject meta;

    public ParagraphDTO(String knowledgeId, String documentId, String title, String content, Integer  position) {
        this.id = IdWorker.get32UUID();
        this.title = title == null ? "" : title;
        this.content = content == null ? "" : content;
        this.status = "nn0";
        this.hitNum = 0;
        this.isActive = true;
        this.knowledgeId = knowledgeId;
        this.documentId = documentId;
        this.position = position==null?1:position;
        this.meta = new JSONObject();
    }
}
