package com.maxkb4j.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatImproveDTO {
    @NotBlank(message = "内容不能为空")
    private String content;
    private String problemText;
    private String title;

}
