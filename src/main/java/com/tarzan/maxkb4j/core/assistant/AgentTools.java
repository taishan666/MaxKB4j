package com.tarzan.maxkb4j.core.assistant;

import dev.langchain4j.agent.tool.Tool;

public class AgentTools {

    @Tool("搜索网络获取实时信息")
    public String search(String query) {
        System.out.println("执行搜索: " + query);
        // 模拟搜索结果
        if (query.toLowerCase().contains("天气")) {
            return "今天天气晴朗，温度22度";
        } else if (query.toLowerCase().contains("首都")) {
            if (query.contains("法国")) return "法国的首都是巴黎";
            if (query.contains("日本")) return "日本的首都是东京";
        } else if (query.toLowerCase().contains("人口")) {
            if (query.contains("中国")) return "中国的人口约为14亿";
            if (query.contains("美国")) return "美国的人口约为3.3亿";
        }
        return "未找到相关信息: " + query;
    }

    @Tool("进行数学计算")
    public double calculate(String expression) {
        System.out.println("执行计算: " + expression);
        // 简单的表达式计算模拟
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            double a = Double.parseDouble(parts[0].trim());
            double b = Double.parseDouble(parts[1].trim());
            return a + b;
        } else if (expression.contains("-")) {
            String[] parts = expression.split("-");
            double a = Double.parseDouble(parts[0].trim());
            double b = Double.parseDouble(parts[1].trim());
            return a - b;
        } else if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            double a = Double.parseDouble(parts[0].trim());
            double b = Double.parseDouble(parts[1].trim());
            return a * b;
        } else if (expression.contains("/")) {
            String[] parts = expression.split("/");
            double a = Double.parseDouble(parts[0].trim());
            double b = Double.parseDouble(parts[1].trim());
            if (b == 0) return Double.NaN;
            return a / b;
        }
        return 0.0;
    }

   // @Tool("获取特定事实信息")
    public String getFact(String topic) {
        System.out.println("获取事实: " + topic);
        return switch (topic.toLowerCase()) {
            case "einstein" -> "阿尔伯特·爱因斯坦是一位著名的理论物理学家，提出了相对论";
            case "darwin" -> "查尔斯·达尔文是进化论的奠基人，著有《物种起源》";
            case "newton" -> "艾萨克·牛顿是经典力学的奠基人，发现了万有引力定律";
            default -> "未知主题: " + topic;
        };
    }
}
