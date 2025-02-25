package com.tarzan.maxkb4j.module.file.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FileVO {
    @JsonProperty("file_id")
    private String fileId;
    private String name;
    private String url;
    private String status;
    private Integer size;
    private Long uid;
}
