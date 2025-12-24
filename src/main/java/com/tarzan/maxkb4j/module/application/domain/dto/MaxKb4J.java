package com.tarzan.maxkb4j.module.application.domain.dto;

import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
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
