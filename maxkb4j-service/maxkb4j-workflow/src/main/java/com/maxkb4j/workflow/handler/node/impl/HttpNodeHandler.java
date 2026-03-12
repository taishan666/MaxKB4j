package com.maxkb4j.workflow.handler.node.impl;

import cn.hutool.http.HttpResponse;
import com.maxkb4j.application.executor.HttpRequestExecutor;
import com.maxkb4j.common.domain.dto.ToolHttpRequest;
import com.maxkb4j.workflow.annotation.NodeHandlerType;
import com.maxkb4j.workflow.enums.NodeType;
import com.maxkb4j.workflow.handler.node.INodeHandler;
import com.maxkb4j.workflow.model.NodeResult;
import com.maxkb4j.workflow.model.Workflow;
import com.maxkb4j.workflow.node.AbsNode;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.HTTP_CLIENT)
@Component
public class HttpNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        Map<String, Object> variables=workflow.getVariableResolver().getPromptVariables();
        HttpRequestExecutor executor= new HttpRequestExecutor(node.getNodeData().toJSONString());
        HttpResponse response=  executor.execute(variables);
        ToolHttpRequest request=executor.getData();
        int resStatus=response.getStatus();
        String resDody=response.body();
        node.getDetail().put("url",request.getUrl());
        node.getDetail().put("method",request.getMethod());
        node.getDetail().put("headers",request.getHeaders());
        node.getDetail().put("requestBody",request);
        node.getDetail().put("params",request.getParams());
        node.getDetail().put("timeout",request.getTimeout());
        return new NodeResult(Map.of("status",resStatus,"body",resDody));
    }

}
