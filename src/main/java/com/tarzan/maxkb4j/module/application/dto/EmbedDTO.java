package com.tarzan.maxkb4j.module.application.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmbedDTO {

    @NotBlank(message = "protocol 不能为空")
    private String protocol;

    @NotBlank(message = "host 不能为空")
    private String host;

    @NotBlank(message = "token 不能为空")
    private String token;

    private String query;
}
