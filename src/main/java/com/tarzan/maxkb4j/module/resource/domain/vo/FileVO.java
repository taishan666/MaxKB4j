package com.tarzan.maxkb4j.module.resource.domain.vo;

import lombok.Data;

@Data
public class FileVO {
    private String fileId;
    private String name;
    private String url;
    private String status;
    private Integer size;
    private Long uid;
}
