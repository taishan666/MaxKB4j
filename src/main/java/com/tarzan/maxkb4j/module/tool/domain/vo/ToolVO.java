package com.tarzan.maxkb4j.module.tool.domain.vo;

import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.module.tool.domain.entity.ToolEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolVO extends ToolEntity {
    private String nickname;
    private List<SysFile> fileList;
}
