package com.tarzan.maxkb4j.module.application.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class PromptGenerateDTO {

    private List<ChatMessageDTO> messages;
    private String prompt;
}
