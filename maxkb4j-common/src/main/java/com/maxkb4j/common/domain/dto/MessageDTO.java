package com.maxkb4j.common.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MessageDTO {
    @NotBlank(message = "内容不能为空")
    private String content;
    @NotBlank(message = "角色不能为空")
    private String role;
}
