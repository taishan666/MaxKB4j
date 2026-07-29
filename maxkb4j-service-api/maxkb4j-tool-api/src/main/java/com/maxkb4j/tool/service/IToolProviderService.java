package com.maxkb4j.tool.service;

import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.mp.entity.KnowledgeSetting;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.skills.shell.ShellSkills;

import java.util.List;

public interface IToolProviderService {

    ShellSkills getShellSkills(List<String> toolIds) throws ApiException;
    List<AiServiceTool> getTools(String userMessage, List<String> toolIds, List<String> applicationIds) throws ApiException;
    List<ToolProvider> getToolProviders(List<String> toolIds, List<String> applicationIds) throws ApiException;
    List<AiServiceTool> getAppTools(List<String> applicationIds) throws ApiException;
    List<AiServiceTool> getKnowledgeTools(List<String> knowledgeIds, KnowledgeSetting knowledgeSetting) throws ApiException;
}
