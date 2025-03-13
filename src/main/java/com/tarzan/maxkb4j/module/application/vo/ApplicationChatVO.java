package com.tarzan.maxkb4j.module.application.vo;

import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationChatVO extends ApplicationChatEntity {
    private Integer chatRecordCount;
    private Integer markSum=0;
}
