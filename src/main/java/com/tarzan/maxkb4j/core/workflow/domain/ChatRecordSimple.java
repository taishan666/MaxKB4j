package com.tarzan.maxkb4j.core.workflow.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatRecordSimple {
    private String question;
    private String answer;
}
