package com.maxkb4j.workflow.model;

import dev.langchain4j.model.input.PromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Template rendering service
 * Responsible for template variable replacement and rendering in workflows
 *
 * @param variableResolver -- GETTER --
 *                         Get variable resolver
 */
@Slf4j
public record TemplateRenderer(VariableResolver variableResolver) {

    /**
     * Render template
     * Replace variable placeholders in the template with actual values
     *
     * @param prompt Template string using {{variable}} format for variable references
     * @return Rendered string
     */
    public String render(String prompt) {
        if (variableResolver == null) {
            log.warn("VariableResolver is null, returning original prompt");
            return prompt != null ? prompt : "";
        }
        return render(prompt, variableResolver.getPromptVariables());
    }

    /**
     * Render template with additional variables
     * Replace variable placeholders in the template with actual values, merging additional variables
     *
     * @param prompt       Template string
     * @param addVariables Additional variable map, will override same-name context variables
     * @return Rendered string
     */
    public String render(String prompt, Map<String, Object> addVariables) {
        if (StringUtils.isBlank(prompt)) {
            return "";
        }
        try {
            Map<String, Object> baseVariables = variableResolver != null
                    ? variableResolver.getPromptVariables()
                    : new HashMap<>();
            Map<String, Object> variables = new HashMap<>(baseVariables);
            if (addVariables != null) {
                variables.putAll(addVariables);
            }
            PromptTemplate promptTemplate = PromptTemplate.from(prompt);
            return promptTemplate.apply(variables).text();
        } catch (Exception e) {
            log.error("Template rendering failed: prompt length={}, error={}", prompt.length(), e.getMessage());
            throw new IllegalArgumentException(
                    String.format("Template rendering failed for prompt (length=%d): %s",
                            prompt.length(), e.getMessage()), e);
        }
    }

}
