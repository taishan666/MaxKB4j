package com.maxkb4j.tool.service;

import com.maxkb4j.common.exception.ApiException;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.shell.ShellSkills;

import java.util.List;

public interface IToolProviderService {

    List<AiServiceTool> getTools(Object chatMemoryId, String userMessage, List<String> toolIds, List<String> applicationIds) throws ApiException;
    List<AiServiceTool> getTools(String chatModelId,List<String> toolIds, List<String> applicationIds) throws ApiException;
    ShellSkills getShellSkills(List<String> toolIds);

    List<ToolProvider> getToolProviders(List<String> toolIds, List<String> applicationIds) throws ApiException;
    List<ToolProvider> getMcpToolProviders(List<String> toolIds) throws ApiException;



}
