package com.maxkb4j.workflow.builder;


import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory class for Compare handlers.
 * Provides centralized handler lookup and registration.
 *
 * Refactored to Spring Bean for consistency with NodeCenter pattern.
 */
@Slf4j
@Component
public class CompareBuilder {

    private final Map<String, Compare> handlerPool = new ConcurrentHashMap<>();

    /**
     * Register a handler for specific comparison operators.
     *
     * @param operators the operators this handler supports
     * @param handler   the compare handler implementation
     * @return true if any existing handler was replaced
     */
    public boolean registerHandler(CompareOperator[] operators, Compare handler) {
        if (operators == null || handler == null) {
            throw new IllegalArgumentException("operators and handler must not be null");
        }
        boolean replaced = false;
        for (CompareOperator operator : operators) {
            if (operator == null) {
                log.warn("Skip null operator in registration");
                continue;
            }
            if (handlerPool.put(operator.getCode(), handler) != null) {
                replaced = true;
                log.debug("Compare handler for '{}' was replaced by {}", operator.getCode(), handler.getClass().getSimpleName());
            } else {
                log.debug("Registered compare handler: {} -> {}", operator.getCode(), handler.getClass().getSimpleName());
            }
        }
        return replaced;
    }

    /**
     * Get a compare handler by operator code.
     *
     * @param operatorCode the comparison operator code (e.g., "eq", "contain")
     * @return the compare handler
     * @throws IllegalArgumentException if no handler is found
     */
    public Compare getHandler(String operatorCode) {
        Compare handler = handlerPool.get(operatorCode);
        if (handler == null) {
            throw new IllegalArgumentException(
                    String.format("No compare handler found for operator: '%s'. Available operators: %s",
                            operatorCode, handlerPool.keySet()));
        }
        return handler;
    }

    /**
     * Check if a handler exists for the given operator code.
     *
     * @param operatorCode the comparison operator code
     * @return true if a handler exists
     */
    public boolean supports(String operatorCode) {
        return handlerPool.containsKey(operatorCode);
    }

    /**
     * Get the number of registered handlers.
     *
     * @return the count of registered handlers
     */
    public int getHandlerCount() {
        return handlerPool.size();
    }
}