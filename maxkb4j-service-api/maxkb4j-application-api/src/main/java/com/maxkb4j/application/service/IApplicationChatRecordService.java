package com.maxkb4j.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.application.dto.ApplicationChatRecordDTO;
import com.maxkb4j.application.vo.ApplicationChatRecordVO;

import java.util.List;

public interface IApplicationChatRecordService  {

    ApplicationChatRecordVO getChatRecordInfo(String chatId, String chatRecordId);

    IPage<ApplicationChatRecordVO> chatRecordPage(String chatId, int current, int size);

    List<ApplicationChatRecordVO> listVOByIds(List<String> ids);

    boolean updateDtoById(ApplicationChatRecordDTO applicationChatDTO);
    List<ApplicationChatRecordDTO> listVoteStatusByChatId(String chatIds);
}
