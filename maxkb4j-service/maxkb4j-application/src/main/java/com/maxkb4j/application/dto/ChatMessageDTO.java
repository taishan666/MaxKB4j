package com.maxkb4j.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessageDTO {
    @NotBlank(message = "内容不能为空")
    private String content;
    @NotBlank(message = "角色不能为空")
    private String role;
}
