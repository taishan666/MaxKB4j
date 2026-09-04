package com.maxkb4j.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.dto.ToolDTO;
import com.maxkb4j.tool.vo.McpToolVO;

import java.util.List;
import java.util.Map;

public interface IToolService {
    ToolDTO getDtoById(String id);
    List<ToolDTO> listDtoByIds(List<String> ids);
    List<Map<String, Object>> listMapsByIds(List<String> ids);
    void saveOrUpdateBatch(List<ToolDTO> toolDTOList,String userId);

    /**
     * 应用导出预处理：将 SKILL 工具的 code（OSS 文件 ID）替换为文件字节的 Base64 编码，
     * 使导出文件自包含、可跨环境导入。
     *
     * @param toolList 工具 DTO 列表
     */
    void embedSkillFileContents(List<ToolDTO> toolList);

    List<McpToolVO> getMcpToolVos(JSONObject mcpServersJson);
}
