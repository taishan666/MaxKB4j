package com.tarzan.maxkb4j.module.chat.dto;

import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import lombok.Data;

import java.util.List;

@Data
public class DataSource {
    private String nodeId;
    private List<ChatFile> fileList;
}