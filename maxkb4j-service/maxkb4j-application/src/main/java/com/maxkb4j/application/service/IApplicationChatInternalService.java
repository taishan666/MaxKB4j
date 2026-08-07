package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.dto.ChatQueryDTO;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 应用对话服务「对内」接口。
 */
public interface IApplicationChatInternalService extends IApplicationChatService, IService<ApplicationChatEntity> {

    IPage<ApplicationChatEntity> chatLogs(String appId, int page, int size, ChatQueryDTO query);

    boolean updateByApplicationId(String appId, String chatId, ApplicationChatEntity chatEntity);

    boolean deleteByApplicationId(String appId, String chatId);

    void chatExport(List<String> ids, HttpServletResponse response) throws IOException;
}