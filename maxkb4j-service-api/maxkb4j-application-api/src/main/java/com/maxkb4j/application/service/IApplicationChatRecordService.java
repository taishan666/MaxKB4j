package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.common.domain.entity.ChatRecordEntity;
import com.maxkb4j.application.vo.ApplicationChatRecordVO;

public interface IApplicationChatRecordService extends IService<ChatRecordEntity> {

    ApplicationChatRecordVO getChatRecordInfo(String chatId, String chatRecordId);

    IPage<ApplicationChatRecordVO> chatRecordPage(String chatId, int current, int size);
}
