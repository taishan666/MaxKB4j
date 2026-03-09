package com.maxkb4j.application.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.maxkb4j.application.dto.ChatQueryDTO;
import com.maxkb4j.application.entity.ApplicationChatEntity;
import com.maxkb4j.application.service.ApplicationChatService;
import com.maxkb4j.common.annotation.SaCheckPerm;
import com.maxkb4j.common.constant.AppConst;
import com.maxkb4j.common.domain.api.R;
import com.maxkb4j.common.enums.PermissionEnum;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@RequestMapping(AppConst.ADMIN_API + "/workspace/default")
@Slf4j
public class ApplicationChatController {

    private final ApplicationChatService chatService;

    @SaCheckPerm(PermissionEnum.APPLICATION_EDIT)
    @PutMapping("/application/{id}/chat/client/{chatId}")
    public R<Boolean> updateChat(@PathVariable("id") String id, @PathVariable("chatId") String chatId, @RequestBody ApplicationChatEntity chatEntity) {
        chatEntity.setId(chatId);
        return R.success(chatService.updateById(chatEntity));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_DELETE)
    @DeleteMapping("/application/{id}/chat/client/{chatId}")
    public R<Boolean> deleteChat(@PathVariable("id") String id, @PathVariable("chatId") String chatId) {
        return R.success(chatService.deleteById(chatId));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @GetMapping("/application/{id}/chat/{page}/{size}")
    public R<IPage<ApplicationChatEntity>> chatLogs(@PathVariable("id") String id, @PathVariable("page") int page, @PathVariable("size") int size, ChatQueryDTO query) {
        return R.success(chatService.chatLogs(id, page, size, query));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_EXPORT)
    @PostMapping("/application/{id}/chat/export")
    public void export(@PathVariable String id, @RequestBody List<String> selectIds, HttpServletResponse response) throws IOException {
        chatService.chatExport(selectIds, response);
    }

}
