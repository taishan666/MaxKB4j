package com.maxkb4j.model.service;


public interface STTModel {
    String speechToText(byte[] audioBytes, String suffix);
}
