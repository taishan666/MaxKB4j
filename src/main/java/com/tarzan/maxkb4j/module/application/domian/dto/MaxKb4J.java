package com.tarzan.maxkb4j.module.application.domian.dto;

import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaxKb4J {
    private ApplicationEntity application;
    private List<ToolEntity> functionLibList;
    private String version;
}
