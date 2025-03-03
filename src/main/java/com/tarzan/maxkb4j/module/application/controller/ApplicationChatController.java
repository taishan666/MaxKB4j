package com.tarzan.maxkb4j.module.application.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.module.application.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.tool.api.R;
import com.tarzan.maxkb4j.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private ApplicationChatService chatService;

    @GetMapping("api/application/{appId}/chat/client/{page}/{size}")
    public R<IPage<ApplicationChatEntity>> clientChatPage(@PathVariable("appId") String appId, @PathVariable("page") int page, @PathVariable("size") int size, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Claims claims = JwtUtil.parseToken(authorization);
        String clientId = (String) claims.get("client_id");
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
    public Flux<JSONObject> chatMessage(@PathVariable String chatId, @RequestBody ChatMessageDTO params, HttpServletRequest request) {
        return chatService.chatMessage(chatId, params, request);
    }

    @GetMapping("api/application/{id}/chat/{chatId}/chat_record/{chatRecordId}")
    public R<ApplicationChatRecordVO> chatRecord(@PathVariable String id, @PathVariable String chatId, @PathVariable String chatRecordId) {
        return R.success(chatService.getChatRecordInfo(chatId, chatRecordId));
    }

    @PutMapping("api/application/{id}/chat/{chatId}/chat_record/{chatRecordId}/vote")
    public R<Boolean> vote(@PathVariable String id, @PathVariable String chatId, @PathVariable String chatRecordId, @RequestBody ApplicationChatRecordEntity chatRecord) {
        return R.success(chatService.getChatRecordVote(chatRecordId, chatRecord));
    }

    @PostMapping("api/application/{id}/chat/{chatId}/upload_file")
    public R<List<JSONObject>> uploadFile(@PathVariable String id, @PathVariable String chatId, MultipartFile[] file) {
        return R.success(chatService.uploadFile(id,chatId,file));
    }

    @PostMapping("api/application/{id}/chat/{chatId}/chat_record/{page}/{size}")
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

    @GetMapping("api/application/{appId}/statistics/chat_record_aggregate_trend")
    public R<List<ApplicationStatisticsVO>> statistics(@PathVariable("appId") String appId, HttpServletRequest request) {
        ChatQueryDTO query = new ChatQueryDTO();
        query.setKeyword(request.getParameter("abstract"));
        query.setStartTime(request.getParameter("start_time"));
        query.setEndTime(request.getParameter("end_time"));
        return R.success(chatService.statistics(appId, query));
    }



}
