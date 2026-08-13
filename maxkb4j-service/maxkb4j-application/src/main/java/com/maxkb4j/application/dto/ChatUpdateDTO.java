package com.maxkb4j.application.dto;

import lombok.Data;

/**
 * 会话更新入参：仅包含客户端可编辑字段。
 *
 * @author tarzan
 */
@Data
public class ChatUpdateDTO {

    private String summary;

    private Integer markSum;
}
