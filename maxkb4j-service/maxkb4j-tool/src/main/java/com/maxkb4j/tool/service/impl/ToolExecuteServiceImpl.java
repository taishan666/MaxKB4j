package com.maxkb4j.tool.service.impl;

import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.dto.ToolInputField;
import com.maxkb4j.tool.executor.GroovyScriptExecutor;
import com.maxkb4j.tool.executor.HttpRequestExecutor;
import com.maxkb4j.tool.executor.McpClientExecutor;
import com.maxkb4j.tool.service.IToolExecuteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具执行服务：负责工具的实际运行（HTTP / 自定义脚本 / MCP），
 * 从工具管理（{@link ToolServiceImpl}）中剥离。
 *
 * @author tarzan
 */
@Service
@Slf4j
public class ToolExecuteServiceImpl implements IToolExecuteService {


    @Override
    public Object httpOrCodeExecute(String toolType, String code, Map<String, Object> initParams, List<ToolInputField> inputFieldList) throws IOException {
        Map<String, Object> inputParams = convertParamType(inputFieldList);
        return httpOrCodeExecute(toolType, code, initParams, inputParams);
    }

    @Override
    public Object httpOrCodeExecute(String toolType, String code, Map<String, Object> initParams, Map<String, Object> parameters) throws IOException {
        log.info("input params: {}", parameters);
        Object result;
        if (ToolConstants.ToolType.HTTP.equals(toolType)) {
            HttpResponse httpResponse = httpExecute(code, initParams, parameters);
            result = httpResponse.body();
        } else {
            result = customExecute(code, initParams, parameters);
        }
        return result;
    }

    public Map<String, Object> convertParamType(List<ToolInputField> inputFieldList) {
        Map<String, Object> inputParams = new HashMap<>(5);
        if (!CollectionUtils.isEmpty(inputFieldList)) {
            for (ToolInputField inputField : inputFieldList) {
                inputParams.put(inputField.getName(), convertValue(inputField.getType(), inputField.getValue()));
            }
        }
        return inputParams;
    }

    /**
     * 将调试字段的字符串 value 按 dataType 转换为对应类型
     *
     * @param dataType 数据类型：string、int、dict、array、float、boolean
     * @param value    原始值（字符串）
     * @return 转换后的值，转换失败时返回原始值
     */
    private Object convertValue(String dataType, Object value) {
        // 仅当 value 是字符串类型时才进行转换，否则原样返回
        if (!(value instanceof String str)) {
            return value;
        }
        if (StringUtils.isBlank(str) || StringUtils.isBlank(dataType)) {
            return value;
        }
        String trimmed = str.trim();
        try {
            return switch (dataType.toLowerCase()) {
                case "int" -> Long.parseLong(trimmed);
                case "float" -> Double.parseDouble(trimmed);
                case "boolean" -> Boolean.parseBoolean(trimmed);
                case "dict" -> JSONUtil.isTypeJSONObject(trimmed) ? JSONUtil.parseObj(trimmed) : value;
                case "array" -> JSONUtil.isTypeJSONArray(trimmed) ? JSONUtil.parseArray(trimmed) : value;
                default -> str;
            };
        } catch (NumberFormatException e) {
            log.warn("Failed to convert debug field value [{}] to type [{}]", str, dataType);
            return value;
        }
    }

    @Override
    public HttpResponse httpExecute(String code, Map<String, Object> initParams, Map<String, Object> parameter) throws IOException {
        HttpRequestExecutor executor = new HttpRequestExecutor(code, initParams);
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