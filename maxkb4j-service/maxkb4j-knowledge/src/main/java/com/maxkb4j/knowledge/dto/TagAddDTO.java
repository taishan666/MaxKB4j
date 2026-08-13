package com.maxkb4j.knowledge.dto;

import lombok.Data;

/**
 * 标签新增入参（第 3 期：Entity 不出入 Controller，替代原 List&lt;TagEntity&gt; 入参）。
 *
 * @author tarzan
 */
@Data
public class TagAddDTO {

    private String key;

    private String value;
}