package com.maxkb4j.tool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ToolFileVO {
    @Schema(description = "文件ID")
    private String id;
    @Schema(description = "文件名称")
    private String name;
    @Schema(description = "文件大小")
    private Long size;
}
