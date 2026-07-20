package com.maxkb4j.knowledge.vo;

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
