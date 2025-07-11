package com.tarzan.maxkb4j.module.application.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.core.workflow.dto.ChatFile;
import com.tarzan.maxkb4j.module.application.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatService;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Tag(name = "APP会话管理", description = "APP会话管理")
@RestController
@RequestMapping(AppConst.BASE_PATH +"/application")
@AllArgsConstructor
public class ApplicationChatController {

    private final ApplicationChatService chatService;

    @GetMapping("/{appId}/chat/client/{page}/{size}")
    public R<IPage<ApplicationChatEntity>> clientChatPage(@PathVariable("appId") String appId, @PathVariable("page") int page, @PathVariable("size") int size) {
        String clientId = (String) StpUtil.getExtra("client_id");
        return R.success(chatService.clientChatPage(appId, clientId, page, size));
    }

    @PutMapping("/{appId}/chat/client/{chatId}")
    public R<Boolean> updateChat(@PathVariable("appId") String appId, @PathVariable("chatId") String chatId, @RequestBody ApplicationChatEntity chatEntity) {
        chatEntity.setId(chatId);
        return R.success(chatService.updateById(chatEntity));
    }

    @DeleteMapping("/{appId}/chat/client/{chatId}")
    public R<Boolean> deleteChat(@PathVariable("appId") String appId, @PathVariable("chatId") String chatId) {
        return R.success(chatService.deleteById(chatId));
    }

    @PostMapping("/chat/open")
    public R<String> chatOpenTest(@RequestBody ApplicationEntity application) {
        return R.success(chatService.chatOpenTest(application));
    }

    @PostMapping("/chat_workflow/open")
    public R<String> chatWorkflowOpenTest(@RequestBody ApplicationEntity application) {
        return R.success(chatService.chatWorkflowOpenTest(application));
    }

    @GetMapping("/{appId}/chat/open")
    public R<String> chatOpen(@PathVariable("appId") String appId) {
        return R.success(chatService.chatOpen(appId));
    }

/*    @PostMapping(path = "/chat_message/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatMessageDTO params) {
        String clientId = (String) StpUtil.getExtra("client_id");
        String clientType = (String) StpUtil.getExtra("client_type");
        params.setClientId(clientId);
        params.setClientType(clientType);
        return chatService.chatMessage(chatId, params);
    }*/

    @PostMapping(path = "/chat_message/{chatId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatMessageVO> chatMessage(@PathVariable String chatId, @RequestBody ChatMessageDTO params) {
        Sinks.Many<ChatMessageVO> sink = Sinks.many().multicast().onBackpressureBuffer();
        String clientId = (String) StpUtil.getExtra("client_id");
        String clientType = (String) StpUtil.getExtra("client_type");
        params.setClientId(clientId);
        params.setClientType(clientType);
        params.setSink(sink);
       // params.setEmitter(emitter);
        new Thread(() -> chatService.chatMessage(chatId, params)).start();
        return sink.asFlux();
    }

    @PutMapping("/{id}/chat/{chatId}/chat_record/{chatRecordId}/vote")
    public R<Boolean> vote(@PathVariable String id, @PathVariable String chatId, @PathVariable String chatRecordId, @RequestBody ApplicationChatRecordEntity chatRecord) {
        return R.success(chatService.getChatRecordVote(chatRecordId, chatRecord));
    }

    @PostMapping("/{id}/chat/{chatId}/upload_file")
    public R<List<ChatFile>> uploadFile(@PathVariable String id, @PathVariable String chatId, MultipartFile[] file) {
        return R.success(chatService.uploadFile(id, chatId, file));
    }

    @GetMapping("/{id}/chat/{chatId}/chat_record/{page}/{size}")
    public R<IPage<ApplicationChatRecordVO>> chatRecordPage(@PathVariable String id, @PathVariable String chatId, @PathVariable int page, @PathVariable int size) {
        return R.success(chatService.chatRecordPage(chatId, page, size));
    }

    @GetMapping("/{appId}/chat/{page}/{size}")
    public R<IPage<ApplicationChatEntity>> chatLogs(@PathVariable("appId") String appId, @PathVariable("page") int page, @PathVariable("size") int size, ChatQueryDTO query) {
        return R.success(chatService.chatLogs(appId, page, size, query));
    }

    @PostMapping("/{id}/chat/export")
    public void export(@PathVariable String id, @RequestBody List<String> selectIds, HttpServletResponse response) throws IOException {
        chatService.chatExport(selectIds, response);
    }

}
