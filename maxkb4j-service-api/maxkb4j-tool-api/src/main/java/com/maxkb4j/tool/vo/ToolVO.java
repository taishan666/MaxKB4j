package com.maxkb4j.tool.vo;

import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.tool.entity.ToolEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolVO extends ToolEntity {
    private String nickname;
    private List<OssFile> fileList;
}
