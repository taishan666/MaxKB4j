package com.maxkb4j.workflow.compare;

import java.util.Collection;

/**
 * Abstract base class for numeric comparison operations.
 * Eliminates duplicate code in GTCompare, GECompare, LTCompare, LECompare.
 *
 * <p>Subclasses only need to implement the compareNumeric method
 * to define their specific comparison logic.</p>
 */
public abstract class AbstractNumericCompare implements Compare {

    /**
     * Compare two numeric values.
     *
     * @param source the source value
     * @param target the target value
     * @return comparison result
     */
    protected abstract boolean compareNumeric(double source, double target);

    @Override
    public boolean compare(Object sourceValue, Object targetValue) {
        if (sourceValue == null || targetValue == null) {
            return false;
        }
        try {
            double sourceNum = toDouble(sourceValue);
            double targetNum = Double.parseDouble(targetValue.toString());
            return compareNumeric(sourceNum, targetNum);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Convert value to double.
     * Supports Number, Collection (size), and String parsing.
     *
     * @param value the value to convert
     * @return double representation
     */
    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof Collection) {
            return ((Collection<?>) value).size();
        }
        return Double.parseDouble(value.toString());
    }
}