package com.maxkb4j.tool.provider;

import com.maxkb4j.tool.annotation.ToolHandlerType;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.executor.GroovyScriptExecutor;
import com.maxkb4j.tool.service.ToolSpecificationBuilder;
import dev.langchain4j.service.tool.ToolExecutor;
import org.springframework.stereotype.Component;

/**
 * Handler for CUSTOM tools: each tool runs a Groovy script executor.
 */
@Component
@ToolHandlerType(ToolConstants.ToolType.CUSTOM)
public class CustomToolHandler extends ExecutorToolHandler {

    public CustomToolHandler(ToolSpecificationBuilder toolSpecificationBuilder) {
        super(toolSpecificationBuilder);
    }

    @Override
    protected ToolExecutor createExecutor(ToolEntity tool) {
        return new GroovyScriptExecutor(tool.getCode(), tool.getInitParams());
    }
}