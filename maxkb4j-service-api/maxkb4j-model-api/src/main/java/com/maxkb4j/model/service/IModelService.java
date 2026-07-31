package com.maxkb4j.model.service;

import com.maxkb4j.model.enums.ModelType;

/**
 * Model service interface
 */
public interface IModelService  {
    String getLastModelId(ModelType modelType);

    String getSafeModelId(String modelId, ModelType modelType);
}
