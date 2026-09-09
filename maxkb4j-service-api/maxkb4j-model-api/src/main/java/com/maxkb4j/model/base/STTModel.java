package com.maxkb4j.model.base;


public interface STTModel {
    String speechToText(byte[] audioBytes, String suffix);
}
