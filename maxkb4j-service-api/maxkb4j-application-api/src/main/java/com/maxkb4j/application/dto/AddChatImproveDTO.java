package com.maxkb4j.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class AddChatImproveDTO {
    private List<String> chatIds;
    private String knowledgeId;
    private String documentId;

}
