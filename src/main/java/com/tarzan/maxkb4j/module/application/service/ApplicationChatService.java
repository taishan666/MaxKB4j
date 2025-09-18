package com.tarzan.maxkb4j.module.application.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.core.exception.ApiException;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeFactory;
import com.tarzan.maxkb4j.core.workflow.domain.ChatFile;
import com.tarzan.maxkb4j.core.workflow.logic.LfNode;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.chat.provider.ChatActuatorBuilder;
import com.tarzan.maxkb4j.module.application.chat.provider.IChatActuator;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationChatRecordEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationKnowledgeMappingEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationVersionEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatRecordDetailVO;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.chat.ChatParams;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Service
@AllArgsConstructor
public class ApplicationChatService extends ServiceImpl<ApplicationChatMapper, ApplicationChatEntity>{

    private final ApplicationKnowledgeMappingService datasetMappingService;
    private final ApplicationVersionService applicationVersionService;
    private final ApplicationChatRecordService chatRecordService;
    private final ApplicationService applicationService;
    private final MongoFileService fileService;


    public IPage<ApplicationChatEntity> chatLogs(String appId, int page, int size, ChatQueryDTO query) {
        Page<ApplicationChatEntity> chatPage = new Page<>(page, size);
        return baseMapper.chatLogs(chatPage,appId,query);
    }



    public String chatOpenTest(String appId) {
        ApplicationVO application = applicationService.getDetail(appId);
        IChatActuator chatActuator= ChatActuatorBuilder.getActuator(application.getType());
        return chatActuator.chatOpenTest(application);
    }


    public String chatOpen(String appId) {
       return chatOpen(appId,null);
    }

    public String chatOpen(String appId,String chatId) {
        ApplicationVO application = applicationService.getDetail(appId);
        if (StringUtils.isBlank(chatId)){
            chatId=IdWorker.get32UUID();
        }
        IChatActuator chatActuator= ChatActuatorBuilder.getActuator(application.getType());
        return chatActuator.chatOpen(application,chatId);
    }

    public ChatInfo reChatOpen(String chatId) {
        ApplicationChatEntity chatEntity = this.getById(chatId);
        if (chatEntity == null){
            return null;
        }
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(chatId);
        ApplicationVO application = applicationService.getDetail(chatEntity.getApplicationId());
        List<ApplicationKnowledgeMappingEntity> list = datasetMappingService.lambdaQuery().eq(ApplicationKnowledgeMappingEntity::getApplicationId, application.getId()).list();
        application.setKnowledgeIdList(list.stream().map(ApplicationKnowledgeMappingEntity::getKnowledgeId).toList());
        chatInfo.setApplication(application);
        ApplicationVersionEntity workFlowVersion = applicationVersionService.lambdaQuery()
                .eq(ApplicationVersionEntity::getApplicationId, application.getId())
                .orderByDesc(ApplicationVersionEntity::getCreateTime)
                .last("limit 1").one();
        if (workFlowVersion != null&&!workFlowVersion.getWorkFlow().isEmpty()){
            LogicFlow logicFlow=LogicFlow.newInstance(workFlowVersion.getWorkFlow());
            List<LfNode> lfNodes=logicFlow.getNodes();
            List<INode> nodes=lfNodes.stream().filter(lfNode -> lfNode.getType().equals("base-node")).map(NodeFactory::getNode).toList();
            chatInfo.setNodes(nodes);
            chatInfo.setEdges(logicFlow.getEdges());
        }
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo;
    }

    private ChatInfo getChatInfo(String chatId) {
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo == null) {
            return reChatOpen(chatId);
        }
        return chatInfo;
    }


    public String chatMessage(ChatParams chatParams) {
        ChatInfo chatInfo = getChatInfo(chatParams.getChatId());
        if (chatInfo == null){
            chatParams.getSink().tryEmitError(new ApiException("会话不存在"));
            return "";
        }
        String appType=chatInfo.getApplication().getType();
        IChatActuator chatActuator= ChatActuatorBuilder.getActuator(appType);
        String answer = chatActuator.chatMessage(chatParams);
        chatParams.getSink().tryEmitComplete();
        return answer;
    }

    public List<ChatFile> uploadFile(String id, String chatId, MultipartFile[] files) {
        List<ChatFile> fileList = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                fileList.add(fileService.uploadFile(file));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return fileList;
    }

    public void chatExport(List<String> ids, HttpServletResponse response) throws IOException {
        List<ChatRecordDetailVO> rows=baseMapper.chatRecordDetail(ids);
        EasyExcel.write(response.getOutputStream(), ChatRecordDetailVO.class).sheet("sheet").doWrite(rows);
    }

    @Transactional
    public Boolean deleteById(String chatId) {
        chatRecordService.lambdaUpdate().eq(ApplicationChatRecordEntity::getChatId, chatId).remove();
        return this.removeById(chatId);
    }
}
