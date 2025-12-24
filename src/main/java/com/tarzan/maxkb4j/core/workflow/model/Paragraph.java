package com.tarzan.maxkb4j.core.workflow.model;

import lombok.Data;

import java.util.List;

@Data
public class Paragraph {
    private String title;
    private String content;
    private Boolean isActive;
    private List<String> problemList;
}
