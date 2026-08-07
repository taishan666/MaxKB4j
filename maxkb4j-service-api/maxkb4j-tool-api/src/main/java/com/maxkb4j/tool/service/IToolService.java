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
    void saveOrUpdateBatch(List<ToolDTO> toolDTOList);
    List<McpToolVO> getMcpToolVos(JSONObject mcpServersJson);
}
