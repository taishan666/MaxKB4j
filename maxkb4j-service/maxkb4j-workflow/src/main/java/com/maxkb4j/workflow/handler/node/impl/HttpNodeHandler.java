package com.maxkb4j.workflow.handler.node.impl;

import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.dto.ToolHttpRequest;
import com.maxkb4j.tool.service.IToolExecuteService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.IWorkflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.HttpNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeHandlerType(NodeType.HTTP_CLIENT)
@Component
@RequiredArgsConstructor
public class HttpNodeHandler extends AbsNodeHandler {

    private static final String MASK = "******";

    private final IToolExecuteService toolExecuteService;

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        HttpNode.NodeParams params = parseParams(node, HttpNode.NodeParams.class);
        Map<String, Object> variables = workflow.getPromptVariables();
        String code = JSON.toJSONString(params);
        ToolHttpRequest request = JSONObject.parseObject(code, ToolHttpRequest.class);
        try (HttpResponse response = toolExecuteService.httpExecute(JSON.toJSONString(params), Map.of(), variables)) {
            int resStatus = response.getStatus();
            String resBody = response.body();
            // 写入详情时隐藏敏感凭据，防止持久化泄露
            Map<String, Object> safeDetails = new HashMap<>();
            safeDetails.put(HttpField.URL, request.getUrl());
            safeDetails.put(HttpField.METHOD, request.getMethod());
            safeDetails.put(HttpField.HEADERS, request.getHeaders());
            // 创建不含凭据的安全请求对象
            ToolHttpRequest safeRequest = new ToolHttpRequest();
            safeRequest.setUrl(request.getUrl());
            safeRequest.setMethod(request.getMethod());
            safeRequest.setBody(request.getBody());
            safeRequest.setHeaders(request.getHeaders());
            safeRequest.setParams(request.getParams());
            safeRequest.setTimeout(request.getTimeout());
            safeRequest.setAuthType(request.getAuthType());
            // 凭据字段用掩码替换
            safeRequest.setUsername(request.getUsername() != null ? MASK : null);
            safeRequest.setPassword(request.getPassword() != null ? MASK : null);
            safeRequest.setToken(request.getToken() != null ? MASK : null);
            safeDetails.put(HttpField.REQUEST_BODY, safeRequest);
            safeDetails.put(HttpField.PARAMS, request.getParams());
            safeDetails.put(HttpField.TIMEOUT, request.getTimeout());
            putDetails(node, safeDetails);
            return new NodeResult(Map.of(HttpField.STATUS, resStatus, HttpField.BODY, resBody));
        }
    }
}