package com.maxkb4j.tool.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.tool.dto.ToolQuery;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.vo.ToolListVO;
import com.maxkb4j.tool.vo.ToolVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 工具服务「对内」接口：供 Controller 使用的完整服务契约。
 *
 * <p>与 {@link IToolService}（对外跨模块契约，位于 maxkb4j-tool-api）区分。
 * 本接口位于 service 模块，可引用 {@link ToolEntity} 等 service 模块类型，
 * 并继承 {@link IService} 获得 MyBatis-Plus 通用方法，使 Controller 依赖抽象而非具体实现。</p>
 */
public interface IToolInternalService extends IToolService, IService<ToolEntity> {

    IPage<ToolVO> pageList(int current, int size, ToolQuery query);

    boolean saveTool(ToolEntity entity);

    boolean mcpServerConfigValid(ToolEntity entity);

    void toolExport(String id, HttpServletResponse response);

    boolean toolImport(MultipartFile file, String folderId);

    boolean testConnection(String code);

    boolean removeToolById(String id);

    List<ToolVO> listTools(String folderId, String scope, String[] toolTypeList);

    List<ToolListVO> toolList(String scope, String toolType);

    ToolVO updateTool(ToolEntity dto) throws IOException;

    ToolVO getVoById(String id);

    String uploadSkillFile(MultipartFile file) throws IOException;

    Boolean delMulApplication(List<String> idList);
}