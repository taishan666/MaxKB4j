package com.maxkb4j.application.vo;

import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationVO extends ApplicationEntity {
    private List<KnowledgeEntity> knowledgeList;
    private String nickname;
    private Boolean showSource;
    private Boolean showExec;
    private String language;
}
