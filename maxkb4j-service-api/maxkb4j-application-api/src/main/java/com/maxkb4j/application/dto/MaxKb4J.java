package com.maxkb4j.application.dto;

import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.tool.entity.ToolEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaxKb4J {
    private ApplicationEntity application;
    private List<ToolEntity> toolList;
    private String version;
}
