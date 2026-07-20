package com.maxkb4j.application.service;

import com.maxkb4j.application.dto.ApplicationAccessTokenDTO;

public interface IApplicationAccessTokenService {
    ApplicationAccessTokenDTO getByAccessToken(String accessToken);
    ApplicationAccessTokenDTO getByAppId(String appId);
}
