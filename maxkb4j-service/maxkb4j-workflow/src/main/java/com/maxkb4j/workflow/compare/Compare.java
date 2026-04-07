package com.maxkb4j.workflow.compare;

/**
 * Interface for comparison operations in workflow conditions.
 * Implementations should be annotated with @CompareType to declare supported operators.
 * Registration is handled automatically by CompareAutoRegistrar.
 */
public interface Compare {

    /**
     * Execute the comparison.
     *
     * @param sourceValue the source value to compare
     * @param targetValue the target value to compare against
     * @return the comparison result
     */
    boolean compare(Object sourceValue, Object targetValue);
}
