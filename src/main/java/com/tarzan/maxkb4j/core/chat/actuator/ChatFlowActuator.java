package com.tarzan.maxkb4j.core.chat.actuator;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.core.workflow.Workflow;
import com.tarzan.maxkb4j.core.workflow.handler.WorkflowHandler;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.chat.dto.ChatParams;
import com.tarzan.maxkb4j.module.chat.dto.ChatResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class ChatFlowActuator implements IChatActuator {

    private final ApplicationChatRecordService chatRecordService;
    private final PostResponseHandler postResponseHandler;
    private final WorkflowHandler workflowHandler;

    @Override
    public ChatResponse chatMessage(ApplicationVO application, ChatParams chatParams) {
        long startTime = System.currentTimeMillis();
        List<ApplicationChatRecordEntity> historyChatRecordList = chatRecordService.getChatRecords(chatParams.getChatId());
        ApplicationChatRecordEntity chatRecord = null;
        if (StringUtil.isNotBlank(chatParams.getChatRecordId())) {
            chatRecord = historyChatRecordList.stream().filter(e -> e.getId().equals(chatParams.getChatRecordId())).findFirst().orElse(null);
        }
        chatParams.setChatRecordId(chatParams.getChatRecordId() == null ? IdWorker.get32UUID() : chatParams.getChatRecordId());
        LogicFlow logicFlow = LogicFlow.newInstance(application.getWorkFlow());
        Workflow workflow= new Workflow(
                logicFlow.getNodes(),
                logicFlow.getEdges(),
                chatParams,
                chatRecord,
                historyChatRecordList);
        String answer = workflowHandler.execute(workflow);
        JSONObject details = workflow.getRuntimeDetails();
        ChatResponse chatResponse = new ChatResponse(answer, details);
        postResponseHandler.handler(chatParams, chatResponse, chatRecord, startTime);
        return chatResponse;
    }

}
