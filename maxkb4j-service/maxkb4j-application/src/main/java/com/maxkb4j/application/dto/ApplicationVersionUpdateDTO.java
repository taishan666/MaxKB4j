package com.maxkb4j.application.dto;

import lombok.Data;

/**
 * 应用版本更新入参：仅包含可编辑的版本元数据。
 *
 * @author tarzan
 */
@Data
public class ApplicationVersionUpdateDTO {

    private String name;

    private String desc;
}
