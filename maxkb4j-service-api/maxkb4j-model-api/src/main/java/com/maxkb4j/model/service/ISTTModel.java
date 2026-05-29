package com.maxkb4j.model.service;


public interface ISTTModel {
    String speechToText(byte[] audioBytes, String suffix);
}
