package com.maxkb4j.model.exception;

/**
 * Exception thrown when a model is not found
 */
public class ModelNotFoundException extends ModelException {

    public ModelNotFoundException(String modelId) {
        super("Model not found: " + modelId);
    }

}