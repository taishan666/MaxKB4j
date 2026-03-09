package com.maxkb4j.application.dto;

import com.maxkb4j.application.entity.ApplicationAccessTokenEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationAccessTokenDTO extends ApplicationAccessTokenEntity {

    private Boolean accessTokenReset;
}
