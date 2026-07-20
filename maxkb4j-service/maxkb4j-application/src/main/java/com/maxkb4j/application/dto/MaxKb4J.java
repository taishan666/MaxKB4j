package com.maxkb4j.application.dto;

import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.tool.dto.ToolDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaxKb4J {
    private ApplicationEntity application;
    private List<ToolDTO> toolList;
    private String version;
}
