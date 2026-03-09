package com.maxkb4j.model.custom.base;


public interface STTModel {
    String speechToText(byte[] audioBytes, String suffix);
}
