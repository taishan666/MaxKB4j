package com.maxkb4j.tool.provider;

import com.maxkb4j.oss.service.IOssService;
import com.maxkb4j.tool.annotation.ToolHandlerType;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.executor.GroovyScriptExecutor;
import com.maxkb4j.tool.service.ToolSpecificationBuilder;
import dev.langchain4j.service.tool.ToolExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Handler for CUSTOM tools: each tool runs a Groovy script executor.
 */
@Component
@ToolHandlerType(ToolConstants.ToolType.CUSTOM)
public class CustomToolHandler extends ExecutorToolHandler {

    private final IOssService ossService;

    public CustomToolHandler(ToolSpecificationBuilder toolSpecificationBuilder, IOssService ossService) {
        super(toolSpecificationBuilder);
        this.ossService = ossService;
    }

    @Override
    protected ToolExecutor createExecutor(ToolEntity tool) {
        return new GroovyScriptExecutor(tool.getCode(), tool.getInitParams(), Map.of("ossService", ossService));
    }
}