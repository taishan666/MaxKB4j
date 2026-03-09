package com.maxkb4j.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class PromptGenerateDTO {

    private List<ChatMessageDTO> messages;
    private String prompt;
}
