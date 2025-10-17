package com.tarzan.maxkb4j.module.model.provider.service.impl;


public abstract class BaseSpeechToText {
    public abstract String speechToText(byte[] audioBytes, String suffix);
}
