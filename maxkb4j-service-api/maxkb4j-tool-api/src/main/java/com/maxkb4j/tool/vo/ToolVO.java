package com.maxkb4j.tool.vo;

import com.maxkb4j.oss.dto.SysFile;
import com.maxkb4j.tool.entity.ToolEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ToolVO extends ToolEntity {
    private String nickname;
    private List<SysFile> fileList;
}
