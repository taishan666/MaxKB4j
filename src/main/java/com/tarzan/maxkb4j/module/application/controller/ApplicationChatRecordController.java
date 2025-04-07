package com.tarzan.maxkb4j.module.application.controller;

import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.vo.ApplicationStatisticsVO;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@AllArgsConstructor
public class ApplicationChatRecordController {

    private final ApplicationChatRecordService chatRecordService;



    @GetMapping("api/application/{id}/chat/{chatId}/chat_record/{chatRecordId}")
    public R<ApplicationChatRecordVO> chatRecord(@PathVariable String id, @PathVariable String chatId, @PathVariable String chatRecordId) {
        return R.success(chatRecordService.getChatRecordInfo(ChatCache.get(chatId), chatRecordId));
    }



    @GetMapping("api/application/{appId}/statistics/chat_record_aggregate_trend")
    public R<List<ApplicationStatisticsVO>> statistics(@PathVariable("appId") String appId, ChatQueryDTO query) {
        return R.success(chatRecordService.statistics(appId, query));
    }



}
