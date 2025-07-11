package com.tarzan.maxkb4j.module.application.domian.entity;

import lombok.Data;

@Data
public class LlmModelSetting {

    private String system;
    private String prompt;
    private String noReferencesPrompt;
}
