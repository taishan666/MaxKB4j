package com.maxkb4j.workflow.compare;

/**
 * Abstract base class for length comparison operations.
 * Eliminates duplicate code in LengthEqualCompare, LengthGTCompare, etc.
 *
 * <p>Subclasses only need to implement the compareLength method
 * to define their specific comparison logic.</p>
 */
public abstract class AbstractLengthCompare implements Compare {

    /**
     * Compare two length values.
     *
     * @param sourceLen the source length
     * @param targetLen the target length
     * @return comparison result
     */
    protected abstract boolean compareLength(int sourceLen, int targetLen);

    @Override
    public boolean compare(Object sourceValue, Object targetValue) {
        if (sourceValue == null || targetValue == null) {
            return false;
        }
        try {
            int sourceLen = getLength(sourceValue);
            int targetLen = Integer.parseInt(targetValue.toString());
            return compareLength(sourceLen, targetLen);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Get length of value.
     * Supports List (size) and String (length).
     *
     * @param value the value to measure
     * @return length value
     */
    private int getLength(Object value) {
        if (value instanceof java.util.List) {
            return ((java.util.List<?>) value).size();
        }
        if (value instanceof String) {
            return ((String) value).length();
        }
        return 0;
    }
}