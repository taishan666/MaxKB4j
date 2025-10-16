package com.tarzan.maxkb4j.core.tool;

import cn.hutool.core.date.DateUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

public class SystemTools {

    @Tool("获取当前时间")
    public String getCurrentTime() {
        return DateUtil.now();
    }

    @Tool("用于执行数学表达式的工具，通过 js 的 expr-eval 库运行表达式并返回结果。")
    public String mathematicalExpression(@P("expression") String expression) {
        Expression engine = new ExpressionBuilder(expression)
                .build();
        double result = engine.evaluate();
        return "result is " + result;
    }


}
