package com.tarzan.maxkb4j.module.application.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Tag(name = "APP会话管理", description = "APP会话管理")
@RestController
@AllArgsConstructor
@RequestMapping(AppConst.ADMIN_API + "/workspace/default")
@Slf4j
public class ApplicationChatController {

    private final ApplicationChatService chatService;


    @PutMapping("/application/{appId}/chat/client/{chatId}")
    public R<Boolean> updateChat(@PathVariable("appId") String appId, @PathVariable("chatId") String chatId, @RequestBody ApplicationChatEntity chatEntity) {
        chatEntity.setId(chatId);
        return R.success(chatService.updateById(chatEntity));
    }

    @DeleteMapping("/application/{appId}/chat/client/{chatId}")
    public R<Boolean> deleteChat(@PathVariable("appId") String appId, @PathVariable("chatId") String chatId) {
        return R.success(chatService.deleteById(chatId));
    }


    @GetMapping("/application/{appId}/chat/{page}/{size}")
    public R<IPage<ApplicationChatEntity>> chatLogs(@PathVariable("appId") String appId, @PathVariable("page") int page, @PathVariable("size") int size, ChatQueryDTO query) {
        return R.success(chatService.chatLogs(appId, page, size, query));
    }

    @PostMapping("/application/{id}/chat/export")
    public void export(@PathVariable String id, @RequestBody List<String> selectIds, HttpServletResponse response) throws IOException {
        chatService.chatExport(selectIds, response);
    }

}
