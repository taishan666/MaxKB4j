package com.tarzan.maxkb4j.module.application.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@AllArgsConstructor
public class ApplicationChatRecordController {

    private final ApplicationChatRecordService chatRecordService;


    @GetMapping("/application/{appId}/chat/{chatId}/chat_record/{chatRecordId}")
    public R<ApplicationChatRecordVO> chatRecord(@PathVariable String appId, @PathVariable String chatId, @PathVariable String chatRecordId) {
        return R.success(chatRecordService.getChatRecordInfo(chatId, chatRecordId));
    }

    @GetMapping("/application/{appId}/application_stats")
    public R<List<ApplicationStatisticsVO>> applicationStats(@PathVariable("appId") String appId, ChatQueryDTO query) {
        return R.success(chatRecordService.statistics(appId, query));
    }

    @GetMapping("/application/{id}/chat/{chatId}/chat_record/{current}/{size}")
    public R<IPage<ApplicationChatRecordVO>> chatRecordPage(@PathVariable String id, @PathVariable String chatId, @PathVariable int current, @PathVariable int size) {
        return R.success(chatRecordService.chatRecordPage(chatId, current, size));
    }




}
