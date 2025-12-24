package com.tarzan.maxkb4j.module.application.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class AddChatImproveDTO {
    private List<String> chatIds;
    private String knowledgeId;
    private String documentId;

}
