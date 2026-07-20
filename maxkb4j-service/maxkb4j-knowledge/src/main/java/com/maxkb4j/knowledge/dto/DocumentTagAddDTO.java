package com.maxkb4j.knowledge.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class DocumentTagAddDTO {

    @NotEmpty(message = "文档ID列表不能为空")
    private List<String> documentIds;
    @NotEmpty(message = "标签ID列表不能为空")
    private List<String> tagIds;
}
