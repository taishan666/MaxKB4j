package com.maxkb4j.tool.service;

import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.dto.ToolDTO;
import com.maxkb4j.tool.vo.McpToolVO;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IToolService {
    ToolDTO getDtoById(String id);
    List<ToolDTO> listDtoByIds(List<String> ids);
    List<Map<String, Object>> listMapsByIds(List<String> ids);
    void saveOrUpdateBatch(List<ToolDTO> toolDTOList);
    List<McpToolVO> getMcpToolVos(JSONObject mcpServersJson);
    HttpResponse httpExecute(String code, Map<String, Object> parameter) throws IOException;
    Object customExecute(String code, Map<String, Object> initParams, Map<String, Object> parameter) throws IOException;
    String mcpToolExecute(String code,String mcpTool, Map<String, Object> parameter) throws IOException;
}
