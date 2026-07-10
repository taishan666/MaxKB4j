package com.maxkb4j.tool.service;

import com.maxkb4j.common.exception.ApiException;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.shell.ShellSkills;

import java.util.List;

public interface IToolProviderService {

    ShellSkills getShellSkills(List<String> toolIds);
    List<AiServiceTool> getTools(String userMessage, List<String> toolIds, List<String> applicationIds) throws ApiException;
    default List<AiServiceTool> getTools(List<String> toolIds, List<String> applicationIds) throws ApiException {
        return getTools(null, toolIds, applicationIds);
    }
    List<ToolProvider> getToolProviders(List<String> toolIds, List<String> applicationIds) throws ApiException;



}
