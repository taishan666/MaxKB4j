package com.tarzan.maxkb4j.module.model.info.exception;

/**
 * Exception thrown when a model is not found
 */
public class ModelNotFoundException extends ModelException {

    public ModelNotFoundException(String modelId) {
        super("Model not found: " + modelId);
    }

    public ModelNotFoundException(String modelId, Throwable cause) {
        super("Model not found: " + modelId, cause);
    }
}