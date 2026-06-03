package com.maxkb4j.knowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class DocumentTagAddDTO {

    private List<String> documentIds;
    private List<String> tagIds;
}
