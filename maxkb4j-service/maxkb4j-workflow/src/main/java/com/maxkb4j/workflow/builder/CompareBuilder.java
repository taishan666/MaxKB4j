package com.maxkb4j.workflow.builder;


import com.maxkb4j.workflow.compare.Compare;
import com.maxkb4j.workflow.enums.CompareOperator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory class for Compare handlers.
 * Provides centralized handler lookup and registration.
 */
@Slf4j
public class CompareBuilder {

    private static final Map<String, Compare> HANDLER_POOL = new ConcurrentHashMap<>();

    /**
     * Register a handler for specific comparison operators.
     *
     * @param operators the operators this handler supports
     * @param handler   the compare handler implementation
     * @return true if any existing handler was replaced
     */
    public static boolean registerHandler(CompareOperator[] operators, Compare handler) {
        if (operators == null || handler == null) {
            throw new IllegalArgumentException("operators and handler must not be null");
        }
        boolean replaced = false;
        for (CompareOperator operator : operators) {
            if (operator == null) {
                log.warn("Skip null operator in registration");
                continue;
            }
            if (HANDLER_POOL.put(operator.getCode(), handler) != null) {
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
    public static Compare getHandler(String operatorCode) {
        Compare handler = HANDLER_POOL.get(operatorCode);
        if (handler == null) {
            throw new IllegalArgumentException(
                    String.format("No compare handler found for operator: '%s'. Available operators: %s",
                            operatorCode, HANDLER_POOL.keySet()));
        }
        return handler;
    }

    /**
     * Check if a handler exists for the given operator code.
     *
     * @param operatorCode the comparison operator code
     * @return true if a handler exists
     */
    public static boolean supports(String operatorCode) {
        return HANDLER_POOL.containsKey(operatorCode);
    }

    /**
     * Get the number of registered handlers.
     *
     * @return the count of registered handlers
     */
    public static int getHandlerCount() {
        return HANDLER_POOL.size();
    }
}