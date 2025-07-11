package com.tarzan.maxkb4j.module.dataset.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class HitTestVO {
    private String paragraphId;
    private Float score;
}
