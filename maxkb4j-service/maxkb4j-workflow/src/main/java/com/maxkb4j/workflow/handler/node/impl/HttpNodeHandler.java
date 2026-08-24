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

import java.util.Map;
import static com.maxkb4j.workflow.consts.WorkflowConstants.*;

@NodeHandlerType(NodeType.HTTP_CLIENT)
@Component
@RequiredArgsConstructor
public class HttpNodeHandler extends AbsNodeHandler {

    private final IToolExecuteService toolExecuteService;

    @Override
    protected NodeResult doExecute(IWorkflow workflow, AbsNode node) throws Exception {
        HttpNode.NodeParams params = parseParams(node, HttpNode.NodeParams.class);
        Map<String, Object> variables = workflow.getPromptVariables();
        String code = JSON.toJSONString(params);
        ToolHttpRequest  request = JSONObject.parseObject(code, ToolHttpRequest.class);
        try (HttpResponse response = toolExecuteService.httpExecute(JSON.toJSONString(params), variables)) {
            int resStatus = response.getStatus();
            String resBody = response.body();
            // 使用辅助方法写入详情
            putDetails(node, Map.of(
                    HttpField.URL, request.getUrl(),
                    HttpField.METHOD, request.getMethod(),
                    HttpField.HEADERS, request.getHeaders(),
                    HttpField.REQUEST_BODY, request,
                    HttpField.PARAMS, request.getParams(),
                    HttpField.TIMEOUT, request.getTimeout()
            ));
            return new NodeResult(Map.of(HttpField.STATUS, resStatus, HttpField.BODY, resBody));
        }
    }
}