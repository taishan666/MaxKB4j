package com.tarzan.maxkb4j.module.dataset.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatasetVO extends DatasetEntity {
    @JsonProperty("char_length")
    private int charLength;
    @JsonProperty("application_mapping_count")
    private int applicationMappingCount;
    @JsonProperty("document_count")
    private int documentCount;
    @JsonProperty("application_id_list")
    private List<String> applicationidList;
}
