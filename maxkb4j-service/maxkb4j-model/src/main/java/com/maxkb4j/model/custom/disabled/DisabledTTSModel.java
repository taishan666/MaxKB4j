package com.maxkb4j.model.custom.disabled;

import com.maxkb4j.model.service.TTSModel;
import dev.langchain4j.model.ModelDisabledException;

public class DisabledTTSModel implements TTSModel {

    @Override
    public byte[] textToSpeech(String text) {
        throw new ModelDisabledException("TTSModel is disabled");
    }
}
