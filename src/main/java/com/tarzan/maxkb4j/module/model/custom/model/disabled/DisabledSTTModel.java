package com.tarzan.maxkb4j.module.model.custom.model.disabled;

import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import dev.langchain4j.model.ModelDisabledException;

public class DisabledSTTModel implements STTModel {
    @Override
    public String speechToText(byte[] audioBytes, String suffix) {
        throw new ModelDisabledException("STTModel is disabled");
    }
}
