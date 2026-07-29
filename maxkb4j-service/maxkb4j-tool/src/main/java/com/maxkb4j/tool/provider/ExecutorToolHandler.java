package com.maxkb4j.tool.provider;

import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.ToolSpecificationBuilder;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.AiServiceTool;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared base for executor-based tool types (HTTP/CUSTOM): builds an
 * {@link AiServiceTool} from a {@link ToolSpecification} + {@link ToolExecutor}, and
 * wraps a batch as a single {@link ToolProvider}. Subclasses only provide the executor.
 */
public abstract class ExecutorToolHandler extends AbsToolHandler {

    protected final ToolSpecificationBuilder toolSpecificationBuilder;

    protected ExecutorToolHandler(ToolSpecificationBuilder toolSpecificationBuilder) {
        this.toolSpecificationBuilder = toolSpecificationBuilder;
    }

    /** Provide the executor for a single tool. */
    protected abstract ToolExecutor createExecutor(ToolEntity tool);

    protected AiServiceTool buildTool(ToolEntity tool) {
        ToolSpecification spec = toolSpecificationBuilder.build(tool);
        return AiServiceTool.builder().toolSpecification(spec).toolExecutor(createExecutor(tool)).build();
    }

    @Override
    public List<AiServiceTool> buildAiServiceTools(ToolEntity tool, String userMessage) {
        return List.of(buildTool(tool));
    }

    @Override
    public List<ToolProvider> buildToolProviders(List<ToolEntity> tools) {
        return List.of(wrapAsToolProvider(tools));
    }

    protected ToolProvider wrapAsToolProvider(List<ToolEntity> tools) {
        return ToolProviderRequest ->{
            List<AiServiceTool> aiServiceTools = new ArrayList<>();
            for (ToolEntity tool : tools) {
                aiServiceTools.add(buildTool(tool));
            }
            return ToolProviderResult.builder().addAll(aiServiceTools).build();
        };
    }
}