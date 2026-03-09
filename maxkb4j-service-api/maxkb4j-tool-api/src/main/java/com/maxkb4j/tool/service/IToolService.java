package com.maxkb4j.tool.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.vo.McpToolVO;
import dev.langchain4j.mcp.client.McpClient;

import java.util.List;

public interface IToolService extends IService<ToolEntity> {

    List<McpToolVO> getMcpToolVos(JSONObject mcpServersJson);

    List<McpClient> getMcpClients(JSONObject mcpServersJson);
}
