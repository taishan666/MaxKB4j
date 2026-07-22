package com.maxkb4j.tool.provider;

import com.maxkb4j.tool.annotation.ToolHandlerType;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.SkillToolService;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Handler for SKILL tools: delegates loading/execution to {@link SkillToolService}.
 */
@Component
@RequiredArgsConstructor
@ToolHandlerType(ToolConstants.ToolType.SKILL)
public class SkillToolHandler extends AbsToolHandler {

    private final SkillToolService skillToolService;

    @Override
    public List<AiServiceTool> buildAiServiceTools(ToolEntity tool, String userMessage) {
        return skillToolService.getSkillsTools(userMessage, tool);
    }

    @Override
    public List<ToolProvider> buildToolProviders(List<ToolEntity> tools) {
        return skillToolService.getShellSkillsToolProviders(tools);
    }
}