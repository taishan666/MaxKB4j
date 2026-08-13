package com.maxkb4j.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 当前用户语言设置入参。
 *
 * @author tarzan
 */
@Data
public class UserLanguageDTO {

    @NotBlank(message = "语言不能为空")
    private String language;
}
