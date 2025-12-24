package com.tarzan.maxkb4j.module.application.domain.dto;

import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationAccessTokenEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationAccessTokenDTO extends ApplicationAccessTokenEntity {

    private Boolean accessTokenReset;
}
