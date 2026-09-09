package com.maxkb4j.tool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Skill 压缩包上传结果：OSS 文件 ID 及从 SKILL.md 中解析出的元信息。
 *
 * @author tarzan
 */
@Data
public class SkillFileVO {
    @Schema(description = "OSS 文件 ID")
    private String fileId;
    @Schema(description = "Skill 名称（SKILL.md front matter 中的 name）")
    private String name;
    @Schema(description = "Skill 描述（SKILL.md front matter 中的 description）")
    private String description;
}
