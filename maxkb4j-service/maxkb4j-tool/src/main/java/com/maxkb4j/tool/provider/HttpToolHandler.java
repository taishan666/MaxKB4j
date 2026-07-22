package com.maxkb4j.tool.provider;

import com.maxkb4j.tool.annotation.ToolHandlerType;
import com.maxkb4j.tool.consts.ToolConstants;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.executor.HttpRequestExecutor;
import com.maxkb4j.tool.service.ToolSpecificationBuilder;
import dev.langchain4j.service.tool.ToolExecutor;
import org.springframework.stereotype.Component;

/**
 * Handler for HTTP tools: each tool is a single HTTP request executor.
 */
@Component
@ToolHandlerType(ToolConstants.ToolType.HTTP)
public class HttpToolHandler extends ExecutorToolHandler {

    public HttpToolHandler(ToolSpecificationBuilder toolSpecificationBuilder) {
        super(toolSpecificationBuilder);
    }

    @Override
    protected ToolExecutor createExecutor(ToolEntity tool) {
        return new HttpRequestExecutor(tool.getCode());
    }
}