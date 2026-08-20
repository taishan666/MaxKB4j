package com.maxkb4j.application.dto;

import lombok.Data;

import java.util.List;

/**
 * 应用 API Key 更新入参：仅包含可编辑字段；
 * secretKey / applicationId / userId 不允许从客户端变更。
 *
 * @author tarzan
 */
@Data
public class ApiKeyUpdateDTO {

    private Boolean isActive;

    private Boolean allowCrossDomain;

    private List<String> crossDomainList;
}
