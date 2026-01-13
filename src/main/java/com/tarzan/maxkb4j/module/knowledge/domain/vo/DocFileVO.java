package com.tarzan.maxkb4j.module.knowledge.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DocFileVO {
    private String name;
    private byte[] bytes;
    private String contentType;
}
