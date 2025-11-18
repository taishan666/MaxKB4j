package com.tarzan.maxkb4j.module.model.custom.base;


public interface STTModel {
    String speechToText(byte[] audioBytes, String suffix);
}
