package com.maxkb4j.knowledge.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class IdListDTO {

    @NotEmpty(message = "ID列表不能为空")
    private List<String> idList;
}
