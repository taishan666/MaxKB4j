package com.maxkb4j.core.executor;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.tool.dto.KeyAndValue;
import com.maxkb4j.tool.dto.ToolHttpRequest;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
public class HttpRequestExecutor extends AbsToolExecutor {
    private final ToolHttpRequest data;

    public HttpRequestExecutor(String code) {
        this.data = JSONObject.parseObject(code, ToolHttpRequest.class);
    }

    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest, Object memoryId) {
        Map<String, Object> variables = argumentsAsMap(toolExecutionRequest.arguments());
        HttpResponse response= execute(variables);
        return response.body();
    }

    public HttpResponse execute(Map<String, Object> variables) {
        HttpRequest request= HttpUtil.createRequest(data.getMethod(), data.getUrl());
        List<KeyAndValue> headers=data.getHeaders();
        for (KeyAndValue header : headers) {
            if (StringUtils.isNotBlank(header.getKey())&& Objects.nonNull(header.getValue())){
                request.header(header.getKey(),header.getValue().toString());
            }
        }
        if(StringUtils.isNotBlank(data.getBody())){
            request.body(renderPrompt(data.getBody(), variables));
        }
        List<KeyAndValue> params=data.getParams();
        for (KeyAndValue param : params) {
            if (StringUtils.isNotBlank(param.getKey())&& Objects.nonNull(param.getValue())){
                request.form(param.getKey(),param.getValue());
            }
        }
        if (StringUtils.isNotBlank(data.getAuthType())){
            switch (data.getAuthType()){
                case "basic":
                    String username=renderPrompt(data.getUsername(), variables);
                    String password=renderPrompt(data.getPassword(), variables);
                    request.basicAuth(username,password);
                    break;
                case "bearer":
                    String token=renderPrompt(data.getToken(), variables);
                    request.bearerAuth(token);
                    break;
            }
        }
        int timeout=data.getTimeout()==null?30:data.getTimeout();
        request.timeout(timeout*1000);
        return request.execute();
    }

    private String renderPrompt(String prompt,Map<String, Object> variables){
        PromptTemplate promptTemplate = PromptTemplate.from(prompt);
        return promptTemplate.apply(variables).text();
    }
}
