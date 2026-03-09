package com.maxkb4j.chat.dto;

import com.maxkb4j.oss.dto.SysFile;
import lombok.Data;

import java.util.List;

@Data
public class DataSource {
    private String nodeId;
    private String sourceUrl;
    private String selector;
    private List<SysFile> fileList;
}