package com.maxkb4j.model.custom.disabled;

import com.maxkb4j.model.service.ISTTModel;
import dev.langchain4j.model.ModelDisabledException;

public class DisabledSTTModel implements ISTTModel {
    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        throw new ModelDisabledException("STTModel is disabled");
    }
}
