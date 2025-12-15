package com.tarzan.maxkb4j.module.knowledge.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class TextChunkVO {
    private String paragraphId;
    private Float score;
}
