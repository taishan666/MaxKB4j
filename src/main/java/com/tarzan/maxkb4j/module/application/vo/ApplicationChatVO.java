package com.tarzan.maxkb4j.module.application.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationChatVO extends ApplicationChatEntity {
    @JsonProperty("chat_record_count")
    private Integer chatRecordCount;
    @JsonProperty("mark_sum")
    private Integer markSum=0;
}
