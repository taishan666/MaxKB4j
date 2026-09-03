package com.maxkb4j.tool.handler;

import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.util.SkillsToolUtil;
import com.maxkb4j.tool.vo.ToolFileVO;
import dev.langchain4j.skills.FileSystemSkill;
import dev.langchain4j.skills.FileSystemSkillLoader;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * 工具 Skill 文件生命周期处理器：负责 SKILL 类型工具的解压、替换、删除及关联文件查询。
 *
 * <p>统一封装 {@link SkillsToolUtil} 与 {@link IOssService} 的协作，非 SKILL 类型为 no-op。
 *
 * @author tarzan
 */
@Component
@RequiredArgsConstructor
public class ToolSkillHandler {

    private final IOssService ossService;

    /** 工具创建时：若为 SKILL，将 oss 压缩包解压到工具目录。 */
    public void onCreate(ToolEntity entity) {
        if (isNotSkill(entity)) {
            return;
        }
        extractSkill(entity.getId(), entity.getCode());
    }

    /** 工具更新时：若为 SKILL 且文件 code 变更，先删旧目录再解压新文件。 */
    public void onUpdate(ToolEntity oldEntity, ToolEntity newEntity) {
        if (isNotSkill(newEntity)) {
            return;
        }
        if (Objects.equals(oldEntity.getCode(), newEntity.getCode())) {
            return;
        }
        SkillsToolUtil.deleteDirectory(oldEntity.getId());
        extractSkill(newEntity.getId(), newEntity.getCode());
    }

    /**
     * 加载 Skill 的 FileSystemSkill，若本地目录不存在则从 OSS 懒加载解压。
     *
     * @param toolId  工具 ID
     * @param fileId  OSS 文件 ID（Skill 压缩包）
     * @return 加载好的 FileSystemSkill
     */
    public FileSystemSkill loadSkill(String toolId, String fileId) throws ApiException {
        Path skillFolder = SkillsToolUtil.getSkillFolder(toolId);
        if (!Files.exists(skillFolder)) {
            extractSkill(toolId, fileId);
        }
        return FileSystemSkillLoader.loadSkill(skillFolder);
    }

    /** 从 OSS 下载并解压 Skill 压缩包到工具目录。 */
    private void extractSkill(String toolId, String fileId) throws ApiException {
        if (StringUtils.isEmpty(toolId) || StringUtils.isEmpty(fileId)) {
            return;
        }
        try (InputStream is = ossService.getStream(fileId)) {
            SkillsToolUtil.unzipSkill(is, toolId);
        } catch (IOException e) {
            throw new ApiException("tool.skill.file.extract.failed");
        }
    }

    /** 工具删除时：若为 SKILL，移除工具目录。 */
    public void onDelete(ToolEntity entity) {
        if (isNotSkill(entity)) {
            return;
        }
        SkillsToolUtil.deleteDirectory(entity.getId());
    }

    /** 组装 VO 时获取 Skill 关联文件列表（非 SKILL 或文件缺失返回空列表，避免前端 NPE）。 */
    public List<ToolFileVO> resolveFileList(ToolEntity entity) {
        if (isNotSkill(entity) || StringUtils.isEmpty(entity.getCode())) {
            return List.of();
        }
        OssFile file = ossService.getFile(entity.getCode());
        if (file == null) {
            return List.of();
        }
        ToolFileVO vo = BeanUtil.copy(file, ToolFileVO.class);
        vo.setId(file.getFileId());
        return List.of(vo);
    }

    private boolean isNotSkill(ToolEntity entity) {
        return !(entity != null && ToolConstants.ToolType.SKILL.equals(entity.getToolType()));
    }

    /** 上传 Skill 压缩包到 oss，返回 fileId。 */
    public String uploadSkillFile(MultipartFile file) throws IOException {
        return ossService.storeFile(file);
    }
}
