package com.tarzan.maxkb4j.module.model.provider;

import java.util.List;

public interface IModelProvider {

    ModelProvideInfo getModelProvideInfo();
    List<ModelInfo> getModelList();
}
