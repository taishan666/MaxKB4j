package com.tarzan.maxkb4j.module.application.domian.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatImproveDTO {
    private List<String> chatIds;
    private String knowledgeId;
    private String documentId;

}
