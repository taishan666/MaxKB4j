package com.tarzan.maxkb4j.core.workflow.node.function.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.node.function.input.FunctionParams;
import org.python.core.*;
import org.python.util.PythonInterpreter;

import java.util.List;
import java.util.Map;

public class BaseFunctionNode extends INode {

    @Override
    public String getType() {
        return "function-node";
    }

    PythonInterpreter pyInterpreter = new PythonInterpreter();
    @Override
    public NodeResult execute() {
        FunctionParams nodeParams=super.nodeParams.toJavaObject(FunctionParams.class);
        String code=nodeParams.getCode();
        String mainPy=code.substring(0,code.indexOf(":")).replace("def","result=");
        List<Map<String,Object>>  inputFieldList=nodeParams.getInputFieldList();
        for (Map<String,Object> map:inputFieldList){
            pyInterpreter.set(map.get("name").toString(),TypeConversion(map.get("value"),map.get("type").toString()));
        }
        // 设置参数值
        code=code  +"\n"+ mainPy;
        // 执行合并后的代码
        pyInterpreter.exec(code);
        // 获取 Python 代码中计算的结果
        PyObject result = pyInterpreter.get("result");
        // 输出结果到 Java 控制台
        return new NodeResult(Map.of("answer",result.toString(),"result",result.toString()),Map.of());
    }

    private PyObject TypeConversion(Object value, String type) {
        if ("int".equals(type)) {
            return new PyInteger(Integer.parseInt(value.toString()));
        } else if ("float".equals(type)) {
            return new PyFloat(Double.parseDouble(value.toString()));
        } else if ("array".equals(type)) {
            return new PyArray((PyType) value);
        } else if ("string".equals(type)) {
            return new PyString(value.toString());
        }else if ("dict".equals(type)) {
            return new PyStringMap((Map<Object, PyObject>) value);
        }
        return null;
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("result",context.get("result"));
        return detail;
    }


    @Override
    public void saveContext(JSONObject nodeDetail, WorkflowManage workflowManage) {

    }
}
