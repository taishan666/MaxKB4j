package com.tarzan.maxkb4j.core.workflow.node.function.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.function.input.FunctionParams;
import com.tarzan.maxkb4j.util.StringUtil;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.FUNCTION;

public class BaseFunctionNode extends INode {
    public BaseFunctionNode() {
        super();
        this.type = FUNCTION.getKey();
    }

    @Override
    public NodeResult execute() {
        System.out.println(FUNCTION);
        FunctionParams nodeParams=super.nodeParams.toJavaObject(FunctionParams.class);
        String code=nodeParams.getCode();
        Object result=null;
        if(StringUtil.isNotBlank(code)){
            List<Map<String,Object>>  inputFieldList=nodeParams.getInputFieldList();
            Binding binding = new Binding();
            StringBuilder sb=new StringBuilder(code);
            sb.append("\n").append("main(");
            for (Map<String,Object> map:inputFieldList){
                String name=map.get("name").toString();
                Object value = map.get("value");
                if (value instanceof JSONArray){
                    List<String> fields=(List<String>)value;
                    value=workflowManage.getReferenceField(fields.get(0),fields.subList(1, fields.size()));
                    if (value!=null&&this.getLastNodeIdList().contains(fields.get(0))){
                        binding.setVariable(name, value);
                        sb.append(name).append(",");
                    }
                }else {
                    binding.setVariable(name, value);
                    sb.append(name).append(",");
                }
            }
            sb.deleteCharAt(sb.length()-1).append(")");
            // 创建 GroovyShell 并运行脚本
            GroovyShell shell = new GroovyShell(binding);
            result = shell.evaluate(sb.toString());
            // 返回结果
            result= result.toString();
        }
        // 输出结果到 Java 控制台
        assert result != null;
        return new NodeResult(Map.of("answer",result,"result",result),Map.of());
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("result",context.get("result"));
        return detail;
    }

}
