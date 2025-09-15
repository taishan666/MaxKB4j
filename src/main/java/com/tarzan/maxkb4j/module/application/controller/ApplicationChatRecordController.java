package com.tarzan.maxkb4j.module.application.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationStatisticsVO;
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
        return R.success(chatRecordService.getChatRecordInfo(ChatCache.get(chatId), chatRecordId));
    }


    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{appId}/statistics/chat_record_aggregate_trend")
    public R<List<ApplicationStatisticsVO>> statistics(@PathVariable("appId") String appId, ChatQueryDTO query) {
        return R.success(chatRecordService.statistics(appId, query));
    }

    @GetMapping("/application/{appId}/application_stats")
    public R<List<ApplicationStatisticsVO>> applicationStats(@PathVariable("appId") String appId, ChatQueryDTO query) {
        return R.success(chatRecordService.statistics(appId, query));
    }



}
