package com.tarzan.maxkb4j.module.application.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.module.application.domain.dto.AddChatImproveDTO;
import com.tarzan.maxkb4j.module.application.domain.dto.ChatImproveDTO;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.ParagraphEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@RequiredArgsConstructor
public class ApplicationChatRecordController {

    private final ApplicationChatRecordService chatRecordService;


    @GetMapping("/application/{id}/chat/{chatId}/chat_record/{chatRecordId}")
    public R<ApplicationChatRecordVO> chatRecord(@PathVariable String id, @PathVariable String chatId, @PathVariable String chatRecordId) {
        return R.success(chatRecordService.getChatRecordInfo(chatId, chatRecordId));
    }



    @GetMapping("/application/{id}/chat/{chatId}/chat_record/{current}/{size}")
    public R<IPage<ApplicationChatRecordVO>> chatRecordPage(@PathVariable String id, @PathVariable String chatId, @PathVariable int current, @PathVariable int size) {
        return R.success(chatRecordService.chatRecordPage(chatId, current, size));
    }

    @PostMapping("/application/{id}/add_knowledge")
    public R<Boolean> addKnowledge(@PathVariable("id") String id, @RequestBody AddChatImproveDTO dto) {
        return R.success(chatRecordService.addChatLogs(id, dto));
    }

    @PutMapping("/application/{id}/chat/{chatId}/chat_record/{chatRecordId}/knowledge/{knowledgeId}/document/{docId}/improve")
    public R<ApplicationChatRecordEntity> improveChatLog(@PathVariable("id") String id, @PathVariable ("chatId") String chatId, @PathVariable ("chatRecordId") String chatRecordId, @PathVariable("knowledgeId") String knowledgeId, @PathVariable ("docId") String docId, @RequestBody ChatImproveDTO dto) {
        return R.success(chatRecordService.improveChatLog(chatId,chatRecordId,knowledgeId,docId, dto));
    }

    @DeleteMapping("/application/{id}/chat/{chatId}/chat_record/{chatRecordId}/knowledge/{knowledgeId}/document/{docId}/paragraph/{paragraphId}/improve")
    public R<Boolean> improveChatLog(@PathVariable("id") String id, @PathVariable ("chatId") String chatId, @PathVariable ("chatRecordId") String chatRecordId, @PathVariable("knowledgeId") String knowledgeId, @PathVariable ("docId") String docId,@PathVariable("paragraphId") String paragraphId) {
        return R.success(chatRecordService.removeImproveChatLog(chatId,chatRecordId,paragraphId));
    }

    @GetMapping("/application/{id}/chat/{chatId}/chat_record/{chatRecordId}/improve")
    public R<List<ParagraphEntity>> improveChatLog(@PathVariable("id") String id, @PathVariable ("chatId") String chatId, @PathVariable ("chatRecordId") String chatRecordId) {
        return R.success(chatRecordService.improveChatLog(chatRecordId));
    }





}
