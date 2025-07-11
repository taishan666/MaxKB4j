package com.tarzan.maxkb4j.module.application.domian.vo;

import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationChatVO extends ApplicationChatEntity {
    private Integer chatRecordCount;
    private Integer markSum=0;
}
