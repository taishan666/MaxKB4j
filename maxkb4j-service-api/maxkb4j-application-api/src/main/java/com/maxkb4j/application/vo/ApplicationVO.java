package com.maxkb4j.application.vo;

import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.common.domain.dto.KnowledgeDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationVO extends ApplicationEntity {
    private List<KnowledgeDTO> knowledgeList;
    private String nickname;
    private Boolean showSource;
    private Boolean showExec;
    private String language;
}
