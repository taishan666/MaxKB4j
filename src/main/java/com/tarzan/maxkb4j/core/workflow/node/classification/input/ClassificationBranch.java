package com.tarzan.maxkb4j.core.workflow.node.classification.input;

import lombok.Data;

@Data
public class ClassificationBranch {
    private String id;
    private String type;
    private String condition;
}
