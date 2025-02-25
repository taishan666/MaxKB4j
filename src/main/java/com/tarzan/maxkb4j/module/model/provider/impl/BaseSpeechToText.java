package com.tarzan.maxkb4j.module.model.provider.impl;

import java.io.File;

public abstract class BaseSpeechToText {
    public abstract String speechToText(byte[] audioFile);
}
