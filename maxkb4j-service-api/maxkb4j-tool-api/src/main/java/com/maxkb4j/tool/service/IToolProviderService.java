package com.maxkb4j.tool.service;

import com.maxkb4j.common.exception.ApiException;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.shell.ShellSkills;

import java.util.List;
import java.util.Map;

public interface IToolProviderService {

    Map<ToolSpecification, ToolExecutor> getToolMap(List<String> toolIds, List<String> applicationIds) throws ApiException;
    ToolProvider getSkillsToolProvider(String applicationId, List<String> toolIds);
    List<ToolProvider> getToolProviders(String applicationId,List<String> toolIds, List<String> applicationIds);
    ToolProvider getSkillsToolProvider(String applicationId, String nodeId, List<String> toolIds);
    String format(ToolExecution toolExecute);
    ShellSkills getShellSkills(String applicationId, List<String> toolIds);
}
