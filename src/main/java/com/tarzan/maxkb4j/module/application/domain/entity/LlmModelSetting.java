package com.tarzan.maxkb4j.module.application.domain.entity;

import lombok.Data;

@Data
public class LlmModelSetting {

    private String system;
    private String prompt;
    private String noReferencesPrompt;
    private Boolean reasoningContentEnable;
}
