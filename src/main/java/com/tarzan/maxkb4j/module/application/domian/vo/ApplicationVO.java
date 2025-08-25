package com.tarzan.maxkb4j.module.application.domian.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.dataset.domain.entity.DatasetEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApplicationVO extends ApplicationEntity {
    private List<String> knowledgeIdList;
    private List<String> mcpIdList;
    private List<String> functionIdList;
    private List<DatasetEntity> knowledgeList;
    private String nickname;
}
