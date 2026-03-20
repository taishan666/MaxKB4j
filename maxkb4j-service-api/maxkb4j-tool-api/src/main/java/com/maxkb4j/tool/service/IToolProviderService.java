package com.maxkb4j.tool.service;

import com.maxkb4j.common.exception.ApiException;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecution;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.List;
import java.util.Map;

public interface IToolProviderService {

    Map<ToolSpecification, ToolExecutor> getToolMap(List<String> toolIds, List<String> applicationIds) throws ApiException;
    String format(ToolExecution toolExecute);
}
