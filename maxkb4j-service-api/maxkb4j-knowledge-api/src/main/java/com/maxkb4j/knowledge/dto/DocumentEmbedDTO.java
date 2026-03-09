package com.maxkb4j.knowledge.dto;

import lombok.Data;

import java.util.List;

@Data
public class DocumentEmbedDTO {
    private List<String> idList;
    private List<String> stateList;
}
