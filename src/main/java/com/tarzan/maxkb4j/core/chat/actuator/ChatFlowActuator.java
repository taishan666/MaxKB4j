package com.tarzan.maxkb4j.core.chat.actuator;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.common.util.StringUtil;
import com.tarzan.maxkb4j.core.chat.base.ChatBaseActuator;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.factory.NodeFactory;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationVersionEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.enums.AppType;
import com.tarzan.maxkb4j.module.application.enums.ChatUserType;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class ChatFlowActuator extends ChatBaseActuator {

    private final ApplicationChatRecordService chatRecordService;
    private final PostResponseHandler postResponseHandler;

    @Override
    public String chatOpenTest(ApplicationVO application) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(IdWorker.get32UUID());
        chatInfo.setAppId(application.getId());
        chatInfo.setAppType(AppType.WORK_FLOW.name());
        chatInfo.setDebug(true);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    @Override
    public String chatOpen(ApplicationVO application, String chatId) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(IdWorker.get32UUID());
        chatInfo.setAppId(application.getId());
        chatInfo.setAppType(AppType.WORK_FLOW.name());
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    @Override
    public String chatMessage(ChatParams chatParams,boolean debug) {
        long startTime = System.currentTimeMillis();
        ChatInfo chatInfo = ChatCache.get(chatParams.getChatId());
        List<ApplicationChatRecordEntity> historyChatRecordList = chatRecordService.getChatRecords(chatInfo, chatParams.getChatId());
        ApplicationChatRecordEntity chatRecord=null;
        if (StringUtil.isNotBlank(chatParams.getChatRecordId())){
            chatRecord=historyChatRecordList.stream().filter(e -> e.getId().equals(chatParams.getChatRecordId())).findFirst().orElse(null);
        }
        chatParams.setChatRecordId(chatParams.getChatRecordId()==null? IdWorker.get32UUID() :chatParams.getChatRecordId());
        ApplicationVO application=super.getAppDetail(chatInfo.getAppId(),debug);
        ApplicationVersionEntity workflowVersion = new ApplicationVersionEntity();
        workflowVersion.setWorkFlow(application.getWorkFlow());
        chatInfo.setAppId(application.getId());
        chatInfo.setAppType(application.getType());
        LogicFlow logicFlow=LogicFlow.newInstance(application.getWorkFlow());
        List<LfNode> lfNodes=logicFlow.getNodes();
        List<INode> nodes=lfNodes.stream().filter(lfNode -> !lfNode.getType().equals("base-node")).map(NodeFactory::getNode).toList();
        WorkflowManage workflowManage = new WorkflowManage(
                nodes,
                logicFlow.getEdges(),
                chatParams,
                chatRecord,
                historyChatRecordList);
        String answer=workflowManage.run();
        JSONObject details= workflowManage.getRuntimeDetails();
        postResponseHandler.handler(chatParams.getChatId(), chatParams.getChatRecordId(), chatParams.getMessage(),answer,chatRecord,details,startTime,chatParams.getUserId(), ChatUserType.ANONYMOUS_USER.name(),debug);
        return answer;
    }

}
