package com.maxkb4j.model.provider;

import com.maxkb4j.model.consts.ProviderModels;
import com.maxkb4j.model.integration.credential.ModelCredentialForm;
import com.maxkb4j.model.vo.ModelInfo;

import java.util.List;

/**
 * Local Model Provider - ONNX local models
 */
public class LocalModelProvider extends AbsModelProvider {

    @Override
    public List<ModelInfo> getModelList() {
        return ProviderModels.LOCAL;
    }

    @Override
    public ModelCredentialForm getModelCredential() {
        return new ModelCredentialForm(false, false);
    }

}
