package com.tarzan.maxkb4j.core.workflow.service;

import dev.langchain4j.model.input.PromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 模板渲染服务
 * 负责工作流中的模板变量替换和渲染
 *
 * @param variableResolver -- GETTER --
 *                         获取变量解析器
 */
@Slf4j
public record TemplateRenderer(VariableResolver variableResolver) {

    /**
     * 渲染模板
     * 将模板中的变量占位符替换为实际值
     *
     * @param prompt 模板字符串，使用 {{variable}} 格式引用变量
     * @return 渲染后的字符串
     */
    public String render(String prompt) {
        return render(prompt, Map.of());
    }

    /**
     * 渲染模板（带额外变量）
     * 将模板中的变量占位符替换为实际值，并合并额外的变量
     *
     * @param prompt       模板字符串
     * @param addVariables 额外的变量映射，会覆盖同名的上下文变量
     * @return 渲染后的字符串
     */
    public String render(String prompt, Map<String, Object> addVariables) {
        if (StringUtils.isBlank(prompt)) {
            return "";
        }
        try {
            Map<String, Object> variables = new HashMap<>(variableResolver.getPromptVariables());
            variables.putAll(addVariables);
            PromptTemplate promptTemplate = PromptTemplate.from(prompt);
            return promptTemplate.apply(variables).text();
        } catch (Exception e) {
            log.error("模板渲染失败: prompt={}", prompt, e);
            return prompt; // 渲染失败时返回原始模板
        }
    }

}
