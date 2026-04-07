package com.maxkb4j.workflow.model.params;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 变量赋值节点参数
 * 从 VariableAssignNode.NodeParams 提取，保持字段定义一致
 */
@Data
public class VariableAssignNodeParams {
    private List<Map<String, Object>> variableList;
}