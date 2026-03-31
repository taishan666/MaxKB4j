package com.maxkb4j.workflow.util;

import com.maxkb4j.workflow.builder.CompareBuilder;
import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.model.Condition;
import com.maxkb4j.workflow.model.Workflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Utility class for evaluating workflow conditions.
 * Refactored to Spring Bean, injecting CompareBuilder for handler lookup.
 */
@Component
@RequiredArgsConstructor
public class ConditionUtil {

    private final CompareBuilder compareBuilder;

    /**
     * Evaluate whether a branch meets the specified conditions.
     *
     * @param workflow      the workflow context
     * @param conditionType "and" or "or" for condition combination
     * @param conditionList the list of conditions to evaluate
     * @return whether the conditions are satisfied
     */
    public boolean assertion(Workflow workflow, String conditionType, List<Condition> conditionList) {
        if (conditionList == null || conditionList.isEmpty()) {
            return true; // No conditions means satisfied
        }
        boolean isAnd = "and".equals(conditionType);
        boolean result = isAnd;
        for (Condition cond : conditionList) {
            boolean conditionResult = assertion(workflow, cond.getField(), cond.getCompare(), cond.getValue());
            if (isAnd) {
                result = conditionResult;
                if (!result) break; // Short-circuit for AND
            } else {
                result = conditionResult;
                if (result) break; // Short-circuit for OR
            }
        }
        return result;
    }

    /**
     * Execute a single condition assertion.
     */
    private boolean assertion(Workflow workflow, List<String> fieldList, String compare, String valueToCompare) {
        if (fieldList == null || fieldList.size() != 2) {
            return false;
        }
        Object fieldValue = workflow.getReferenceField(fieldList);
        try {
            Compare handler = compareBuilder.getHandler(compare);
            return handler.compare(fieldValue, valueToCompare);
        } catch (IllegalArgumentException e) {
            // Unknown comparison operator
            return false;
        }
    }
}