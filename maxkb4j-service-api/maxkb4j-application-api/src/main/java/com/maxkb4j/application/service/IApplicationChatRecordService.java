package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.maxkb4j.application.entity.ApplicationChatRecordEntity;
import com.maxkb4j.application.vo.ApplicationChatRecordVO;

import java.util.List;

public interface IApplicationChatRecordService extends IService<ApplicationChatRecordEntity> {

    ApplicationChatRecordVO getChatRecordInfo(String chatId, String chatRecordId);

    IPage<ApplicationChatRecordVO> chatRecordPage(String chatId, int current, int size);

    List<ApplicationChatRecordVO> listVOByIds(List<String> ids);
}
