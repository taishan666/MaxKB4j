package com.tarzan.maxkb4j.module.application.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.aop.SaCheckPerm;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.application.domain.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
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
