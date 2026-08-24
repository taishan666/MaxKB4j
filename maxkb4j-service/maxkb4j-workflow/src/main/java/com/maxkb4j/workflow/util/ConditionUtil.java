package com.maxkb4j.workflow.util;

import com.maxkb4j.workflow.enums.CompareOperator;
import com.maxkb4j.workflow.model.Condition;
import com.maxkb4j.workflow.model.IWorkflow;

import java.util.List;
import static com.maxkb4j.workflow.consts.WorkflowConstants.LogicField;

/**
 * Utility for evaluating workflow branch conditions.
 * Stateless: comparison semantics live in {@link CompareOperator} itself.
 */
public final class ConditionUtil {

    private static final String AND = LogicField.AND;

    private ConditionUtil() {
    }

    /**
     * Evaluate whether a branch meets the specified conditions.
     *
     * @param workflow      the workflow context
     * @param conditionType "and" or "or" for condition combination
     * @param conditionList the list of conditions to evaluate
     * @return whether the conditions are satisfied
     */
    public static boolean assertion(IWorkflow workflow, String conditionType, List<Condition> conditionList) {
        if (conditionList == null || conditionList.isEmpty()) {
            return true;
        }
        if (AND.equals(conditionType)) {
            return conditionList.stream().allMatch(condition -> matches(workflow, condition));
        }
        return conditionList.stream().anyMatch(condition -> matches(workflow, condition));
    }

    /**
     * Execute a single condition assertion.
     */
    private static boolean matches(IWorkflow workflow, Condition condition) {
        List<String> field = condition.getField();
        if (field == null || field.size() != 2) {
            return false;
        }
        CompareOperator operator = CompareOperator.fromCode(condition.getCompare());
        return operator != null && operator.compare(workflow.getReferenceField(field), condition.getValue());
    }
}
