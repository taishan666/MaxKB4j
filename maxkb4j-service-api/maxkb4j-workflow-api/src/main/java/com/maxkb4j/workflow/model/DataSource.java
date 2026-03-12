package com.maxkb4j.workflow.model;

import com.maxkb4j.common.domain.dto.OssFile;
import lombok.Data;

import java.util.List;

@Data
public class DataSource {
    private String nodeId;
    private String sourceUrl;
    private String selector;
    private List<OssFile> fileList;
}