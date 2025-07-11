package com.tarzan.maxkb4j.module.application.domian.vo;

import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationVO extends ApplicationEntity {
    private List<String> datasetIdList;
    private List<String> mcpIdList;
    private String model;
    private String sttModel;
    private String ttsModel;
}
