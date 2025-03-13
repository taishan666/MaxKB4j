package com.tarzan.maxkb4j.module.application.vo;

import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationVO extends ApplicationEntity {
    private List<String> datasetIdList;
    private String model;
    private String sttModel;
    private String ttsModel;
}
