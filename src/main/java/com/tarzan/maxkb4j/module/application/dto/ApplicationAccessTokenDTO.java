package com.tarzan.maxkb4j.module.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tarzan.maxkb4j.module.application.entity.ApplicationAccessTokenEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationAccessTokenDTO extends ApplicationAccessTokenEntity {

    private Boolean accessTokenReset;
}
