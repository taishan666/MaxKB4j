package com.tarzan.maxkb4j.core.chat.actuator;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class ChatFlowActuator implements IChatActuator {

    private final ApplicationChatRecordService chatRecordService;
    private final PostResponseHandler postResponseHandler;

    @Override
    public String chatMessage(ApplicationVO application, ChatParams chatParams) {
        long startTime = System.currentTimeMillis();
        List<ApplicationChatRecordEntity> historyChatRecordList = chatRecordService.getChatRecords(chatParams.getChatId());
        ApplicationChatRecordEntity chatRecord = null;
        if (StringUtil.isNotBlank(chatParams.getChatRecordId())) {
            chatRecord = historyChatRecordList.stream().filter(e -> e.getId().equals(chatParams.getChatRecordId())).findFirst().orElse(null);
        }
        chatParams.setChatRecordId(chatParams.getChatRecordId() == null ? IdWorker.get32UUID() : chatParams.getChatRecordId());
        LogicFlow logicFlow = LogicFlow.newInstance(application.getWorkFlow());
        WorkflowManage workflowManage = new WorkflowManage(
                logicFlow.getNodes(),
                logicFlow.getEdges(),
                chatParams,
                chatRecord,
                historyChatRecordList);
        String answer = workflowManage.run();
        JSONObject details = workflowManage.getRuntimeDetails();
        postResponseHandler.handler(chatParams.getChatId(), chatParams.getChatRecordId(), chatParams.getMessage(), answer, chatRecord, details, startTime, chatParams.getChatUserId(), chatParams.getChatUserType(), chatParams.getDebug());
        return answer;
    }

}
