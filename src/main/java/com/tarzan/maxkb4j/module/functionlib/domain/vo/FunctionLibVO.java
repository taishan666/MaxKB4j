package com.tarzan.maxkb4j.module.functionlib.domain.vo;

import com.tarzan.maxkb4j.module.functionlib.domain.entity.FunctionLibEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class FunctionLibVO extends FunctionLibEntity {
    private String nickname;
}
