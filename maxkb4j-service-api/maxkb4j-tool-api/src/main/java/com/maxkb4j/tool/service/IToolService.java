package com.maxkb4j.tool.service;

import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.vo.McpToolVO;
import com.maxkb4j.tool.vo.ToolVO;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface IToolService extends IService<ToolEntity> {

    List<McpToolVO> getMcpToolVos(JSONObject mcpServersJson);

    ToolVO updateTool(ToolEntity dto) throws IOException;
    HttpResponse httpExecute(String code, Map<String, Object> parameter) throws IOException;
    Object customExecute(String code, Map<String, Object> initParams, Map<String, Object> parameter) throws IOException;
    String mcpToolExecute(String code,String mcpTool, Map<String, Object> parameter) throws IOException;
}
