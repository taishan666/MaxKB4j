package com.tarzan.maxkb4j.core.workflow.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Schema(description = "对话文件对象")
@Data
public class SysFile {
    @Schema(description = "文件ID")
    private String fileId;
    @Schema(description = "文件名称")
    private String name;
    @Schema(description = "文件类型")
    private String type;
    @Schema(description = "文件URL")
    private String url;
    @Schema(description = "文件大小")
    private Long size;
    @Schema(description = "上传时间")
    private Date uploadTime;
}
