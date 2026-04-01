package com.maxkb4j.workflow.handler.node.impl;

import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.maxkb4j.application.executor.HttpRequestExecutor;
import com.maxkb4j.common.domain.dto.ToolHttpRequest;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.AbstractNodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.model.params.HttpNodeParams;
import com.maxkb4j.workflow.node.AbsNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.HTTP_CLIENT)
@Component
public class HttpNodeHandler extends AbstractNodeHandler {

    @Override
    protected NodeResult doExecute(Workflow workflow, AbsNode node) throws Exception {
        HttpNodeParams params = parseParams(node, HttpNodeParams.class);
        Map<String, Object> variables = workflow.getPromptVariables();
        HttpRequestExecutor executor = new HttpRequestExecutor(JSON.toJSONString(params));
        HttpResponse response = executor.execute(variables);
        ToolHttpRequest request = executor.getData();
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