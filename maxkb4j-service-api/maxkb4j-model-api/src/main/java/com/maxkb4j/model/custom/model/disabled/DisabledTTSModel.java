package com.maxkb4j.model.custom.model.disabled;

import com.maxkb4j.model.custom.base.TTSModel;
import dev.langchain4j.model.ModelDisabledException;

public class DisabledTTSModel implements TTSModel {

    @Override
    public byte[] textToSpeech(String text) {
        throw new ModelDisabledException("TTSModel is disabled");
    }
}
