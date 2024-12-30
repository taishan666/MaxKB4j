package com.tarzan.maxkb4j.module.application.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationVO extends ApplicationEntity {
    @JsonProperty("dataset_id_list")
    private List<UUID> datasetIdList;
}
