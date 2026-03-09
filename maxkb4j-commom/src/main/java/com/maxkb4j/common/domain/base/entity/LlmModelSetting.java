package com.maxkb4j.common.domain.base.entity;

import lombok.Data;

@Data
public class LlmModelSetting {

    private String system;
    private String prompt;
    private String noReferencesPrompt;
    private Boolean reasoningContentEnable;
}
