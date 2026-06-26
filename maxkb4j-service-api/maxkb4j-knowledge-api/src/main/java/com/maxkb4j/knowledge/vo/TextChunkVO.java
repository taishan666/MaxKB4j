package com.maxkb4j.knowledge.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class TextChunkVO {
    private String paragraphId;
    private Double score;
}
