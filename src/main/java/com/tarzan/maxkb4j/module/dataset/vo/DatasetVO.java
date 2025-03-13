package com.tarzan.maxkb4j.module.dataset.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatasetVO extends DatasetEntity {
    private int charLength;
    private int applicationMappingCount;
    private int documentCount;
    private List<String> applicationidList;
}
