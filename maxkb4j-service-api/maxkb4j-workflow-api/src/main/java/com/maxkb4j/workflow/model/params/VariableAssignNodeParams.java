package com.maxkb4j.workflow.model.params;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 变量赋值节点参数
 * 从 VariableAssignNode.NodeParams 提取
 */
@Data
public class VariableAssignNodeParams {
    private List<VariableAssign> variableAssignList;

    @Data
    public static class VariableAssign {
        private String variableName;
        private String source;
        private Object value;
        private List<String> reference;
    }
}