package com.tarzan.maxkb4j.module.application.domain.dto;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationDTO extends ApplicationEntity {

    private JSONObject  workFlowTemplate;
}
