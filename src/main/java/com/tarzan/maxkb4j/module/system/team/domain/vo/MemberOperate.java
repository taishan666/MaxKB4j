package com.tarzan.maxkb4j.module.system.team.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MemberOperate {
    @JsonProperty("MANAGE")
    private Boolean manage;
    @JsonProperty("USE")
    private Boolean use;
}
