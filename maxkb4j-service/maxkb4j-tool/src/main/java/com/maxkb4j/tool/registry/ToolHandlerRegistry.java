package com.maxkb4j.tool.registry;

import com.maxkb4j.tool.annotation.ToolHandlerType;
import com.maxkb4j.tool.provider.AbsToolHandler;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool handler registry: keyed by {@link ToolHandlerType#value()}, holds all tool
 * handler beans. Registration happens at startup (see
 * {@link com.maxkb4j.tool.processor.ToolHandlerAutoRegistrar}); afterwards read-only.
 * Replaces the former if/else dispatch on toolType.
 */
@Getter
@Component
public class ToolHandlerRegistry {

    private final Map<String, AbsToolHandler> handlers = new LinkedHashMap<>();

    public void register(AbsToolHandler handler, ToolHandlerType annotation) {
        handlers.put(annotation.value(), handler);
    }

    /** Get the handler for a tool type, or null if none registered. */
    public AbsToolHandler get(String toolType) {
        if (toolType == null || toolType.isEmpty()) {
            return null;
        }
        return handlers.get(toolType);
    }
}