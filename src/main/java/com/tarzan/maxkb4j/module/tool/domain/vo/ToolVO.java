package com.tarzan.maxkb4j.module.tool.domain.vo;

import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolVO extends ToolEntity {
    private String nickname;
}
