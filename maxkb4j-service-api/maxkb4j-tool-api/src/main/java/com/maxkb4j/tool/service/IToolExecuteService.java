package com.maxkb4j.tool.service;

import cn.hutool.http.HttpResponse;
import com.maxkb4j.tool.dto.ToolInputField;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 工具执行跨模块契约：仅包含工具运行（HTTP / 自定义脚本 / MCP）能力，
 * 与 {@link IToolService}（工具管理查询契约）分离。
 */
public interface IToolExecuteService {

    Map<String, Object> convertParamType(List<ToolInputField> inputFieldList);

    Object httpOrCodeExecute(String toolType,String code, Map<String, Object> initParams, List<ToolInputField> inputFieldList) throws IOException;

    Object httpOrCodeExecute(String toolType,String code, Map<String, Object> initParams, Map<String, Object> parameter) throws IOException;

    HttpResponse httpExecute(String code, Map<String, Object> initParams,Map<String, Object> parameter) throws IOException;

    Object customExecute(String code, Map<String, Object> initParams, Map<String, Object> parameter) throws IOException;

    String mcpToolExecute(String code, String mcpTool, Map<String, Object> parameter) throws IOException;
}