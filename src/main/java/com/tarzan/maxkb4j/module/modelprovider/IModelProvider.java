package com.tarzan.maxkb4j.module.modelprovider;

import java.util.List;

public interface IModelProvider {

    ModelProvideInfo getModelProvideInfo();
    List<ModelInfo> getModelList();
}
