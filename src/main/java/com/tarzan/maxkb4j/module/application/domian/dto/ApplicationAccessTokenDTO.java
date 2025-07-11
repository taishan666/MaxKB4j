package com.tarzan.maxkb4j.module.application.domian.dto;

import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationAccessTokenEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationAccessTokenDTO extends ApplicationAccessTokenEntity {

    private Boolean accessTokenReset;
}
