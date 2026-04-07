package com.maxkb4j.application.executor;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.domain.dto.KeyAndValue;
import com.maxkb4j.common.domain.dto.ToolHttpRequest;
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
        ToolHttpRequest  tempData = JSONObject.parseObject(code, ToolHttpRequest.class);
        List<KeyAndValue>  headers =tempData.getHeaders();
        List<KeyAndValue>  params =tempData.getParams();
        // 3. 过滤 Headers
        if (headers != null) {
            headers.removeIf(this::isEmptyKeyAndValue);
        }

        // 4. 过滤 Params
        if (params != null) {
            params.removeIf(this::isEmptyKeyAndValue);
        }
        this.data = tempData;
    }

    private boolean isEmptyKeyAndValue(KeyAndValue kav) {
        if (kav == null) return true; // 如果列表中包含 null 元素，也移除

        boolean isKeyEmpty = (kav.getKey() == null || kav.getKey().trim().isEmpty());
        boolean isValueEmpty = (kav.getValue() == null || kav.getValue().toString().trim().isEmpty());

        // 只有当 key 和 value 都为空时，才返回 true (表示需要被移除)
        return isKeyEmpty && isValueEmpty;
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
                header.setKey(renderPrompt(header.getKey(), variables));
                header.setValue(renderPrompt(String.valueOf(header.getValue()), variables));
                request.header(header.getKey(),String.valueOf(header.getValue()));
            }
        }
        if(StringUtils.isNotBlank(data.getBody())){
            data.setBody(renderPrompt(data.getBody(), variables));
            request.body(data.getBody());
        }
        List<KeyAndValue> params=data.getParams();
        for (KeyAndValue param : params) {
            if (StringUtils.isNotBlank(param.getKey())&& Objects.nonNull(param.getValue())){
                param.setKey(renderPrompt(param.getKey(), variables));
                param.setValue(renderPrompt(String.valueOf(param.getValue()), variables));
                request.form(param.getKey(),param.getValue());
            }
        }
        if (StringUtils.isNotBlank(data.getAuthType())){
            switch (data.getAuthType()){
                case "basic":
                    data.setUsername(renderPrompt(data.getUsername(), variables));
                    data.setPassword(renderPrompt(data.getPassword(), variables));
                    request.basicAuth(data.getUsername(),data.getPassword());
                    break;
                case "bearer":
                    data.setToken(renderPrompt(data.getToken(), variables));
                    request.bearerAuth(data.getToken());
                    break;
            }
        }
        data.setTimeout(data.getTimeout()==null?30:data.getTimeout());
        request.timeout(data.getTimeout()*1000);
        return request.execute();
    }

    private String renderPrompt(String prompt,Map<String, Object> variables){
        if (StringUtils.isNotBlank(prompt)){
            PromptTemplate promptTemplate = PromptTemplate.from(prompt);
            return promptTemplate.apply(variables).text();
        }
        return  "";
    }
}
