package com.tarzan.maxkb4j.module.chat.dto;

import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import lombok.Data;

import java.util.List;

@Data
public class DataSource {
    private String nodeId;
    private String sourceUrl;
    private String selector;
    private List<SysFile> fileList;
}