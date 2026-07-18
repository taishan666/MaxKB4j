package com.maxkb4j.chat.query;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmbedQuery {

    @NotBlank(message = "protocol 不能为空")
    private String protocol;

    @NotBlank(message = "host 不能为空")
    private String host;

    @NotBlank(message = "token 不能为空")
    private String token;

    private String query;
}
