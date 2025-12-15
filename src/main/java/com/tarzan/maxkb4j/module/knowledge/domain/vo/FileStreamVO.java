package com.tarzan.maxkb4j.module.knowledge.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@RequiredArgsConstructor
@NoArgsConstructor
@Data
public class FileStreamVO {
    private String name;
    private InputStream inputStream;
}
