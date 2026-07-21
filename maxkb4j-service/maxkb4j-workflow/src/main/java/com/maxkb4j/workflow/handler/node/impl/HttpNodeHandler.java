package com.maxkb4j.workflow.handler.node.impl;

import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.dto.ToolHttpRequest;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbsNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import com.maxkb4j.workflow.node.impl.HttpNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.HTTP_CLIENT)
@Component
@RequiredArgsConstructor
public class HttpNodeHandler extends AbsNodeHandler {

    private final IToolService toolService;

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        HttpNode.NodeParams params = parseParams(node, HttpNode.NodeParams.class);
        Map<String, Object> variables = workflow.getPromptVariables();
        String code = JSON.toJSONString(params);
        ToolHttpRequest  request = JSONObject.parseObject(code, ToolHttpRequest.class);
        HttpResponse response = toolService.httpExecute(JSON.toJSONString(params),variables);
        int resStatus = response.getStatus();
        String resBody = response.body();
        // 使用辅助方法写入详情
        putDetails(node, Map.of(
                "url", request.getUrl(),
                "method", request.getMethod(),
                "headers", request.getHeaders(),
                "requestBody", request,
                "params", request.getParams(),
                "timeout", request.getTimeout()
        ));
        return new NodeResult(Map.of("status", resStatus, "body", resBody));
    }
}