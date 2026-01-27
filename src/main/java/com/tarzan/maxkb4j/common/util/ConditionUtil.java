package com.tarzan.maxkb4j.common.util;

import com.tarzan.maxkb4j.core.workflow.compare.Compare;
import com.tarzan.maxkb4j.core.workflow.compare.impl.*;
import com.tarzan.maxkb4j.core.workflow.model.Condition;
import com.tarzan.maxkb4j.core.workflow.model.Workflow;

import java.util.ArrayList;
import java.util.List;

public class ConditionUtil {

    private static final List<Compare> COMPARE_HANDLERS = new ArrayList<>();

    static {
        COMPARE_HANDLERS.add(new GECompare());
        COMPARE_HANDLERS.add(new GTCompare());
        COMPARE_HANDLERS.add(new ContainCompare());
        COMPARE_HANDLERS.add(new EqualCompare());
        COMPARE_HANDLERS.add(new LTCompare());
        COMPARE_HANDLERS.add(new LECompare());
        COMPARE_HANDLERS.add(new LengthLECompare());
        COMPARE_HANDLERS.add(new LengthLTCompare());
        COMPARE_HANDLERS.add(new LengthEqualCompare());
        COMPARE_HANDLERS.add(new LengthGECompare());
        COMPARE_HANDLERS.add(new LengthGTCompare());
        COMPARE_HANDLERS.add(new IsNullCompare());
        COMPARE_HANDLERS.add(new IsNotNullCompare());
        COMPARE_HANDLERS.add(new NotContainCompare());
        COMPARE_HANDLERS.add(new IsTrueCompare());
        COMPARE_HANDLERS.add(new IsNotTrueCompare());
    }

    /**
     * 判断某个分支是否满足条件
     *
     * @param workflow 工作流上下文
     * @param branch   分支定义
     * @return 是否匹配该分支
     */
    public static boolean assertion(Workflow workflow, String conditionType, List<Condition> conditionList) {
        if (conditionList == null || conditionList.isEmpty()) {
            return true; // 无条件视为满足（可按需调整）
        }
        boolean isAnd = "and".equals(conditionType);
        boolean result = isAnd;
        for (Condition cond : conditionList) {
            boolean conditionResult = assertion(workflow, cond.getField(), cond.getCompare(), cond.getValue());
            if (isAnd) {
                result = conditionResult;
                if (!result) break; // 短路优化
            } else {
                result = conditionResult;
                if (result) break; // 短路优化
            }
        }
        return result;
    }

    /**
     * 执行单个条件的断言判断
     */
    private static boolean assertion(Workflow workflow, List<String> fieldList, String compare, String valueToCompare) {
        if (fieldList == null || fieldList.size() != 2) {
            return false;
        }
        Object fieldValue = workflow.getReferenceField(fieldList);
        for (Compare handler : COMPARE_HANDLERS) {
            if (handler.support(compare)) {
                return handler.compare(fieldValue, valueToCompare);
            }
        }
        return false;
    }
}