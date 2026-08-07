package com.maxkb4j.tool.service.impl;

import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.executor.GroovyScriptExecutor;
import com.maxkb4j.tool.executor.HttpRequestExecutor;
import com.maxkb4j.tool.executor.McpClientExecutor;
import com.maxkb4j.tool.service.IToolExecuteService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * 工具执行服务：负责工具的实际运行（HTTP / 自定义脚本 / MCP），
 * 从工具管理（{@link ToolServiceImpl}）中剥离。
 *
 * @author tarzan
 */
@Service
public class ToolExecuteServiceImpl implements IToolExecuteService {

    @Override
    public HttpResponse httpExecute(String code, Map<String, Object> parameter) throws IOException {
        HttpRequestExecutor executor = new HttpRequestExecutor(code);
        return executor.execute(parameter);
    }

    @Override
    public Object customExecute(String code, Map<String, Object> initParams, Map<String, Object> parameter) throws IOException {
        GroovyScriptExecutor scriptExecutor = new GroovyScriptExecutor(code, initParams);
        return scriptExecutor.execute(parameter);
    }

    @Override
    public String mcpToolExecute(String code, String mcpTool, Map<String, Object> parameter) throws IOException {
        McpClientExecutor mcpClientExecutor = new McpClientExecutor(code);
        return mcpClientExecutor.execute(mcpTool, new JSONObject(parameter));
    }
}