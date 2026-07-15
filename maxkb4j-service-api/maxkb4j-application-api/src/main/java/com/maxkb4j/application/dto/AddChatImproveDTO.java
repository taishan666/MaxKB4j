package com.maxkb4j.application.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddChatImproveDTO {
    @NotEmpty(message = "聊天记录ID列表不能为空")
    private List<String> chatIds;
    private String knowledgeId;
    private String documentId;

}
