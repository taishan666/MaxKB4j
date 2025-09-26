package com.tarzan.maxkb4j.core.workflow.model;

import lombok.Data;

import java.util.Date;

@Data
public class ChatFile {
    private String fileId;
    private String name;
    private String type;
    private String url;
    private Long size;
    private Date uploadTime;
}
