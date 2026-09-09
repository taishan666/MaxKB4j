package com.maxkb4j.application.dto;

import com.maxkb4j.common.domain.dto.MessageDTO;
import lombok.Data;

import java.util.List;

@Data
public class PromptGenerateDTO {

    private List<MessageDTO> messages;
    private String prompt;
}
