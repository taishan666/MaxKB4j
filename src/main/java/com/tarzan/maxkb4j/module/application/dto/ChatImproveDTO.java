package com.tarzan.maxkb4j.module.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ChatImproveDTO {
    @JsonProperty("chat_ids")
    private List<String> chatIds;
    @JsonProperty("dataset_id")
    private String datasetId;
    @JsonProperty("document_id")
    private String documentId;

}
