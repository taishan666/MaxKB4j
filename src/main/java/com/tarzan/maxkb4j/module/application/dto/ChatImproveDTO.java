package com.tarzan.maxkb4j.module.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ChatImproveDTO {
    private List<String> chatIds;
    private String datasetId;
    private String documentId;

}
