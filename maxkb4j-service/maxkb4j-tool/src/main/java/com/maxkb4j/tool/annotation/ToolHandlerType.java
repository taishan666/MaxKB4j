package com.maxkb4j.tool.annotation;

import java.lang.annotation.*;

/**
 * Marks an {@link com.maxkb4j.tool.provider.AbsToolHandler} implementation as
 * auto-discoverable, keyed by tool type. Scanned at startup by
 * {@link com.maxkb4j.tool.processor.ToolHandlerAutoRegistrar} and registered into
 * {@link com.maxkb4j.tool.registry.ToolHandlerRegistry}, replacing the former
 * if/else dispatch on toolType in ToolProviderServiceImpl.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ToolHandlerType {
    /** Tool type key, matching {@code ToolConstants.ToolType} / {@code ToolEntity#getToolType()}. */
    String value();
}