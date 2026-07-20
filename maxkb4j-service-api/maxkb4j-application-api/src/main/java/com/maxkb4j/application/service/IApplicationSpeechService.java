package com.maxkb4j.application.service;

import com.alibaba.fastjson.JSONObject;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IApplicationSpeechService {
    String speechToText(String appId, MultipartFile file, boolean debug) throws IOException;
    byte[] textToSpeech(String appId, JSONObject data, boolean debug);
}
