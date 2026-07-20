package com.maxkb4j.application.service;

import com.maxkb4j.application.dto.ApplicationApiKeyDTO;

public interface IApplicationApiKeyService{
    ApplicationApiKeyDTO getBySecretKey(String secretKey);
}
