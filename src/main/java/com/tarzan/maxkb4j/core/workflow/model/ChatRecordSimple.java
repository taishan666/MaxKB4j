package com.tarzan.maxkb4j.core.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@RequiredArgsConstructor
@NoArgsConstructor
@Data
public class ChatRecordSimple {
    private String question;
    private String answer;
}
