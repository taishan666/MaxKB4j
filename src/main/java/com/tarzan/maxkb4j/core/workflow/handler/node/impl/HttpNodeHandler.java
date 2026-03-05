package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import cn.hutool.http.HttpResponse;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.module.tool.domain.dto.ToolHttpRequest;
import com.tarzan.maxkb4j.module.tool.executor.HttpRequestExecutor;
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
