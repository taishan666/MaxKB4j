package com.tarzan.maxkb4j.module.application.controller;

import org.springframework.web.bind.annotation.RestController;
import lombok.AllArgsConstructor;
import com.tarzan.maxkb4j.module.application.service.ApplicationApiKeyService;
/**
 * @author tarzan
 * @date 2025-01-02 09:01:12
 */
@RestController
@AllArgsConstructor
public class ApplicationApiKeyController{

	private	final ApplicationApiKeyService applicationApiKeyService;
}
