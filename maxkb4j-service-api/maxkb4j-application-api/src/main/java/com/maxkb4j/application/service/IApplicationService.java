package com.maxkb4j.application.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.dto.EmbedDTO;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.vo.ApplicationVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IApplicationService extends IService<ApplicationEntity> {

    String speechToText(String appId, MultipartFile file, boolean debug) throws IOException;
    byte[] textToSpeech(String appId, JSONObject data, boolean debug);
    String embed(EmbedDTO dto);
    ApplicationVO appProfile(String appId);
}
