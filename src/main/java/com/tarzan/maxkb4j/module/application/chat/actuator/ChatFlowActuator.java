package com.tarzan.maxkb4j.module.application.chat.actuator;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeFactory;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.domain.FlowParams;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.chat.base.ChatBaseActuator;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationVersionEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.service.ApplicationChatRecordService;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.application.service.ApplicationVersionService;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class ChatFlowActuator extends ChatBaseActuator {

    private final ApplicationVersionService applicationVersionService;
    private final ApplicationChatMapper chatMapper;
    private final ApplicationService applicationService;
    private final ApplicationChatRecordService chatRecordService;
    private final PostResponseHandler postResponseHandler;

    @Override
    public String chatOpenTest(ApplicationVO application) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(IdWorker.get32UUID());
        application.setId(null);
        ApplicationVersionEntity workflowVersion = new ApplicationVersionEntity();
        workflowVersion.setWorkFlow(application.getWorkFlow());
        chatInfo.setApplication(application);
        LogicFlow logicFlow=LogicFlow.newInstance(application.getWorkFlow());
        List<LfNode> lfNodes=logicFlow.getNodes();
        List<INode> nodes=lfNodes.stream().filter(lfNode -> !lfNode.getType().equals("base-node")).map(NodeFactory::getNode).toList();
        chatInfo.setNodes(nodes);
        chatInfo.setEdges(logicFlow.getEdges());
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    @Override
    public String chatOpen(ApplicationVO application, String chatId) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(chatId);
        JSONObject workFlow=application.getWorkFlow();
        if (workFlow == null){
            ApplicationVersionEntity workFlowVersion = applicationVersionService.lambdaQuery()
                    .eq(ApplicationVersionEntity::getApplicationId, application.getId())
                    .orderByDesc(ApplicationVersionEntity::getCreateTime)
                    .last("limit 1").one();
            workFlow=workFlowVersion.getWorkFlow();
        }
        //todo workFlow为null时，处理
        chatInfo.setApplication(application);
        LogicFlow logicFlow=LogicFlow.newInstance(workFlow);
        List<LfNode> lfNodes=logicFlow.getNodes();
        List<INode> nodes=lfNodes.stream().filter(lfNode -> !lfNode.getType().equals("base-node")).map(NodeFactory::getNode).toList();
        chatInfo.setNodes(nodes);
        chatInfo.setEdges(logicFlow.getEdges());
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatId;
    }

    @Override
    public String chatMessage(ChatMessageDTO dto) {
        long startTime = System.currentTimeMillis();
        ChatInfo chatInfo = getChatInfo(dto.getChatId());
        chatCheck(chatInfo,dto);
        ApplicationChatRecordEntity chatRecord = null;
        String chatRecordId = dto.getChatRecordId();
        if(StringUtils.isNotBlank(chatRecordId)){
            chatRecord = chatRecordService.getChatRecordEntity(chatInfo, chatRecordId);
        }
        FlowParams flowParams = new FlowParams();
        flowParams.setChatId(chatInfo.getChatId());
        flowParams.setChatRecordId(dto.getChatRecordId() == null ? IdWorker.get32UUID() : dto.getChatRecordId());
        flowParams.setQuestion(dto.getMessage());
        flowParams.setReChat(dto.getReChat());
        flowParams.setClientId(dto.getClientId());
        flowParams.setClientType(dto.getClientType());
        flowParams.setStream(dto.getStream() == null || dto.getStream());
        flowParams.setHistoryChatRecord(chatInfo.getChatRecordList());//添加历史记录
        WorkflowManage workflowManage = new WorkflowManage(
                chatInfo.getNodes(),
                chatInfo.getEdges(),
                flowParams,
                dto.getSink(),
                dto.getGlobalData(),
                dto.getImageList(),
                dto.getDocumentList(),
                dto.getAudioList(),
                dto.getRuntimeNodeId(),
                dto.getNodeData(),
                chatRecord);
        String answer=workflowManage.run();
        JSONObject details= workflowManage.getRuntimeDetails();
        postResponseHandler.handler(flowParams.getChatId(), flowParams.getChatRecordId(), flowParams.getQuestion(),answer,chatRecord,details,startTime,flowParams.getClientId(),flowParams.getClientType(),dto.isDebug());
        return answer;
    }

    @Override
    public ChatInfo reChatOpen(String chatId) {
        ApplicationChatEntity chatEntity = chatMapper.selectById(chatId);
        if (chatEntity == null){
            return null;
        }
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(chatId);
        ApplicationVO application = applicationService.getDetail(chatEntity.getApplicationId());
        chatInfo.setApplication(application);
        ApplicationVersionEntity workFlowVersion = applicationVersionService.lambdaQuery()
                .eq(ApplicationVersionEntity::getApplicationId, application.getId())
                .orderByDesc(ApplicationVersionEntity::getCreateTime)
                .last("limit 1").one();
        LogicFlow logicFlow=LogicFlow.newInstance(workFlowVersion.getWorkFlow());
        List<LfNode> lfNodes=logicFlow.getNodes();
        List<INode> nodes=lfNodes.stream().filter(lfNode -> lfNode.getType().equals("base-node")).map(NodeFactory::getNode).toList();
        chatInfo.setNodes(nodes);
        chatInfo.setEdges(logicFlow.getEdges());
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo;
    }
}
