package com.tarzan.maxkb4j.core.workflow.handler.node.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.annotation.NodeHandlerType;
import com.tarzan.maxkb4j.core.workflow.enums.NodeType;
import com.tarzan.maxkb4j.core.workflow.handler.node.INodeHandler;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;
import com.tarzan.maxkb4j.core.workflow.node.AbsNode;
import com.tarzan.maxkb4j.core.workflow.node.impl.HttpNode;
import com.tarzan.maxkb4j.core.workflow.model.NodeResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

@NodeHandlerType(NodeType.HTTP_CLIENT)
@Component
public class HttpNodeHandler implements INodeHandler {

    @Override
    public NodeResult execute(Workflow workflow, AbsNode node) throws Exception {
        HttpNode.NodeParams nodeParams=node.getNodeData().toJavaObject(HttpNode.NodeParams.class);
        String url=nodeParams.getUrl();
        String resDody="";
        int resStatus=100;
        if (url.startsWith("http")||url.startsWith("https")){
            HttpRequest request= HttpUtil.createRequest(nodeParams.getMethod(), url);
            node.getDetail().put("url",url);
            node.getDetail().put("method",nodeParams.getMethod());
            JSONArray headers=nodeParams.getHeaders();
            for (int i = 0; i < headers.size(); i++) {
                JSONObject header=headers.getJSONObject(i);
                if (!header.isEmpty()){
                    String name=header.getString("name");
                    String value=header.getString("value");
                    name=workflow.renderPrompt(name);
                    value=workflow.renderPrompt(value);
                    request.header(name,value);
                }
            }
            node.getDetail().put("headers",request.headers());
            String body=nodeParams.getBody();
            node.getDetail().put("requestBody",body);
            if (StringUtils.isNotBlank(body)){
                body=workflow.renderPrompt(body);
                request.body(body);
            }
            JSONArray params=nodeParams.getParams();
            for (int i = 0; i < params.size(); i++) {
                JSONObject param=params.getJSONObject(i);
                if (!param.isEmpty()){
                    String name=param.getString("name");
                    String value=param.getString("value");
                    name=workflow.renderPrompt(name);
                    value=workflow.renderPrompt(value);
                    request.form(name,value);
                }
            }
            node.getDetail().put("params",request.form());
            if (StringUtils.isNotBlank(nodeParams.getAuthType())){
                switch (nodeParams.getAuthType()){
                    case "basic":
                        String username=workflow.renderPrompt(nodeParams.getUsername());
                        String password=workflow.renderPrompt(nodeParams.getPassword());
                        request.basicAuth(username,password);
                        break;
                    case "bearer":
                        String token=workflow.renderPrompt(nodeParams.getToken());
                        request.bearerAuth(token);
                        break;
                }
            }
            int timeout=nodeParams.getTimeout()==null?30:nodeParams.getTimeout();
            request.timeout(timeout);
            node.getDetail().put("timeout",nodeParams.getTimeout());
            HttpResponse response=request.execute();
            resDody=response.body();
            resStatus=response.getStatus();
        }
        return new NodeResult(Map.of("status",resStatus,"body",resDody));
    }
}
