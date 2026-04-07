package com.maxkb4j.workflow.model.params;

import com.maxkb4j.workflow.model.Condition;
import lombok.Data;

import java.util.List;

/**
 * 条件节点参数
 * 从 ConditionNode.NodeParams 提取
 */
@Data
public class ConditionNodeParams {
    private List<Branch> branch;

    @Data
    public static class Branch {
        private String id;
        private String type;
        private String condition;
        private List<Condition> conditions;
    }
}