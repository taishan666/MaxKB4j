package com.maxkb4j.model.custom.disabled;

import com.maxkb4j.model.service.STTModel;
import dev.langchain4j.model.ModelDisabledException;

public class DisabledSTTModel implements STTModel {
    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        throw new ModelDisabledException("STTModel is disabled");
    }
}
