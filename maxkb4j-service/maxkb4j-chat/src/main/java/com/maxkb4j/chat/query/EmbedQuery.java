package com.maxkb4j.chat.query;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EmbedQuery {

    @NotBlank(message = "protocol 不能为空")
    private String protocol;

    @NotBlank(message = "host 不能为空")
    private String host;

    @NotBlank(message = "token 不能为空")
    private String token;

    private Map<String, Object> params=Map.of();
}
