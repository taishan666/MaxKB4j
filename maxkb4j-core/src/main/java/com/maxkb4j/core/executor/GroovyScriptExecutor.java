package com.maxkb4j.core.executor;

import com.alibaba.fastjson.JSON;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class GroovyScriptExecutor extends AbsToolExecutor {


    private final String code;
    private final Map<String, Object> initParams;

    public GroovyScriptExecutor(String code, Map<String, Object> initParams) {
        this.code = code;
        this.initParams=initParams;
    }

    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest,  Object memoryId) {
        Map<String, Object> params = argumentsAsMap(toolExecutionRequest.arguments());
        Object value= execute(params);
        return JSON.toJSONString(value);
    }

    public Object execute(Map<String, Object> params) {
        Object result="";
        if(StringUtils.isNotBlank(code)){
            if (initParams!=null){
                params.putAll(initParams);
            }
            Binding binding = new Binding(params);
            // 创建 GroovyShell 并执行脚本
            GroovyShell shell = new GroovyShell(binding);
            result = shell.evaluate(code);
            result=result==null?"":result;
        }
        return result;
    }
}
