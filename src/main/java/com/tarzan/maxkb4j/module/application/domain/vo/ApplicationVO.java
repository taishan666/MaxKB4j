package com.tarzan.maxkb4j.module.application.domain.vo;

import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationVO extends ApplicationEntity {
    private List<String> knowledgeIdList;
    private List<KnowledgeEntity> knowledgeList;
    private String nickname;
    private Boolean showSource;
    private Boolean showExec;
    private String language;
}
