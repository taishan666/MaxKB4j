package com.maxkb4j.knowledge.dto;

import lombok.Data;

/**
 * 段落更新入参：仅包含客户端可编辑字段。
 *
 * @author tarzan
 */
@Data
public class ParagraphUpdateDTO {

    private String title;

    private String content;

    private Boolean isActive;
}
