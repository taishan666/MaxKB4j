package com.maxkb4j.tool.service;

import com.maxkb4j.common.exception.ApiException;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.*;

import java.util.List;
import java.util.Map;

public interface IToolProviderService {

    List<AiServiceTool> getTools(List<String> toolIds, List<String> applicationIds) throws ApiException;
    Map<ToolSpecification, ToolExecutor> getToolMap(List<String> toolIds, List<String> applicationIds) throws ApiException;
    ToolProvider getSkillsProvider(String modelId, List<String> toolIds);
    String format(BeforeToolExecution toolExecute);
    String format(ToolExecution toolExecute);
    ToolProvider getSkillsProvider(List<String> toolIds);

}
