package com.tarzan.maxkb4j.module.application.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.module.application.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import com.tarzan.maxkb4j.tool.api.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@AllArgsConstructor
public class ApplicationChatController {

    private final ApplicationChatService chatService;

    @GetMapping("api/application/{appId}/chat/client/{page}/{size}")
    public R<IPage<ApplicationChatEntity>> clientChatPage(@PathVariable("appId") String appId, @PathVariable("page") int page, @PathVariable("size") int size, HttpServletRequest request) {
        String clientId = (String) StpUtil.getExtra("client_id");
        return R.success(chatService.clientChatPage(appId, clientId, page, size));
    }

    @PostMapping("api/application/chat/open")
    public R<String> chatOpenTest(@RequestBody ApplicationEntity application) {
        return R.success(chatService.chatOpenTest(application));
    }

    @PostMapping("api/application/chat_workflow/open")
    public R<String> chatWorkflowOpenTest(@RequestBody ApplicationEntity application) {
        return R.success(chatService.chatWorkflowOpenTest(application));
    }

    @GetMapping("api/application/{appId}/chat/open")
    public R<String> chatOpen(@PathVariable("appId") String appId) {
        return R.success(chatService.chatOpen(appId));
    }

    @PostMapping(path = "api/application/chat_message/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatMessageDTO params, HttpServletRequest request) {
        return chatService.chatMessage(chatId, params, request);
    }

    @PutMapping("api/application/{id}/chat/{chatId}/chat_record/{chatRecordId}/vote")
    public R<Boolean> vote(@PathVariable String id, @PathVariable String chatId, @PathVariable String chatRecordId, @RequestBody ApplicationChatRecordEntity chatRecord) {
        return R.success(chatService.getChatRecordVote(chatRecordId, chatRecord));
    }

    @PostMapping("api/application/{id}/chat/{chatId}/upload_file")
    public R<List<JSONObject>> uploadFile(@PathVariable String id, @PathVariable String chatId, MultipartFile[] file) {
        return R.success(chatService.uploadFile(id,chatId,file));
    }

    @GetMapping("api/application/{id}/chat/{chatId}/chat_record/{page}/{size}")
    public R<IPage<ApplicationChatRecordVO>> chatRecordPage(@PathVariable String id, @PathVariable String chatId, @PathVariable int page, @PathVariable int size) {
        return R.success(chatService.chatRecordPage(chatId, page, size));
    }

    @GetMapping("api/application/{appId}/chat/{page}/{size}")
    public R<IPage<ApplicationChatEntity>> chatLogs(@PathVariable("appId") String appId, @PathVariable("page") int page, @PathVariable("size") int size, HttpServletRequest request) {
        ChatQueryDTO query = new ChatQueryDTO();
        query.setKeyword(request.getParameter("abstract"));
        query.setStartTime(request.getParameter("start_time"));
        query.setEndTime(request.getParameter("end_time"));
        return R.success(chatService.chatLogs(appId, page, size, query));
    }

    @PostMapping("api/application/{id}/chat/export")
    public void export(@PathVariable String id, HttpServletResponse response) {
         chatService.chatExport(id,response);
    }

}
