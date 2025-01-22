package com.tarzan.maxkb4j.module.application.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationVO extends ApplicationEntity {
    @JsonProperty("dataset_id_list")
    private List<String> datasetIdList;
    private String model;
    @JsonProperty("stt_model")
    private String sttModel;
    @JsonProperty("tts_model")
    private String ttsModel;
}
