package com.maxkb4j.tool.service;

import com.maxkb4j.common.exception.ApiException;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.skills.shell.ShellSkills;

import java.util.List;

public interface IToolProviderService {

    List<AiServiceTool> getTools(String chatModelId,List<String> toolIds, List<String> applicationIds) throws ApiException;
    ShellSkills getShellSkills(List<String> toolIds);

}
