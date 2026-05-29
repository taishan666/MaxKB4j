package com.maxkb4j.model.custom.disabled;

import com.maxkb4j.model.service.ITTSModel;
import dev.langchain4j.model.ModelDisabledException;

public class DisabledTTSModel implements ITTSModel {

    @Override
    public byte[] textToSpeech(String text) {
        throw new ModelDisabledException("TTSModel is disabled");
    }
}
