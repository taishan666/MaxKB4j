package com.maxkb4j.tool.provider;

import com.maxkb4j.tool.entity.ToolEntity;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.List;

/**
 * Base contract for tool handlers, one per tool type (MCP/HTTP/SKILL/CUSTOM).
 * Replaces the former if/else dispatch on toolType in ToolProviderServiceImpl:
 * adding a new tool type only requires a new {@code @ToolHandlerType} implementation.
 */
public abstract class AbsToolHandler {

    /**
     * Build {@link AiServiceTool}s for a single tool (used when assembling an AiService).
     *
     * @param tool         the tool entity
     * @param userMessage  the user message (used by SKILL type tools)
     * @return the built tools (empty list by default)
     */
    public List<AiServiceTool> buildAiServiceTools(ToolEntity tool, String userMessage) {
        return List.of();
    }

    /**
     * Build {@link ToolProvider}s for a batch of tools of the same type.
     *
     * @param tools the tools of a single type
     * @return the built providers (empty list by default)
     */
    public List<ToolProvider> buildToolProviders(List<ToolEntity> tools) {
        return List.of();
    }
}