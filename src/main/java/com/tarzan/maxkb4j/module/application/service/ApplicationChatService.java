package com.tarzan.maxkb4j.module.application.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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
import com.tarzan.maxkb4j.module.application.domian.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.*;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ChatRecordDetailVO;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.resource.service.MongoFileService;
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

    private final ApplicationMapper applicationMapper;
    private final ApplicationDatasetMappingService datasetMappingService;
    private final ApplicationWorkFlowVersionService workFlowVersionService;
    private final ApplicationChatRecordService chatRecordService;
    private final MongoFileService fileService;


    public IPage<ApplicationChatEntity> chatLogs(String appId, int page, int size, ChatQueryDTO query) {
        Page<ApplicationChatEntity> chatPage = new Page<>(page, size);
        return baseMapper.chatLogs(chatPage,appId,query);
    }

    public IPage<ApplicationChatEntity> clientChatPage(String appId,String clientId, int page, int size) {
        Page<ApplicationChatEntity> chatPage = new Page<>(page, size);
        LambdaQueryWrapper<ApplicationChatEntity> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApplicationChatEntity::getApplicationId,appId);
        wrapper.eq(ApplicationChatEntity::getClientId, clientId);
        wrapper.orderByDesc(ApplicationChatEntity::getCreateTime);
        return this.page(chatPage, wrapper);
    }

    public String chatOpenTest(ApplicationEntity application) {
        IChatActuator chatActuator= ChatActuatorBuilder.getActuator(application.getType());
        return chatActuator.chatOpenTest(application);
    }

/*    public String chatWorkflowOpenTest(ApplicationEntity application) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(IdWorker.get32UUID());
        application.setId(null);
        ApplicationWorkFlowVersionEntity workflowVersion = new ApplicationWorkFlowVersionEntity();
        workflowVersion.setWorkFlow(application.getWorkFlow());
        application.setDialogueNumber(3);
        application.setType(AppType.WORKFLOW.name());
        application.setUserId(StpUtil.getLoginIdAsString());
        chatInfo.setApplication(application);
        chatInfo.setWorkFlowVersion(workflowVersion);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }*/

    public String chatOpen(String appId) {
       return chatOpen(appId,null);
    }

    public String chatOpen(String appId,String chatId) {
        ApplicationEntity application = applicationMapper.selectById(appId);
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
        ApplicationEntity application = applicationMapper.selectById(chatEntity.getApplicationId());
        List<ApplicationDatasetMappingEntity> list = datasetMappingService.lambdaQuery().eq(ApplicationDatasetMappingEntity::getApplicationId, application.getId()).list();
        application.setDatasetIdList(list.stream().map(ApplicationDatasetMappingEntity::getDatasetId).toList());
        chatInfo.setApplication(application);
        ApplicationWorkFlowVersionEntity workFlowVersion = workFlowVersionService.lambdaQuery()
                .eq(ApplicationWorkFlowVersionEntity::getApplicationId, application.getId())
                .orderByDesc(ApplicationWorkFlowVersionEntity::getCreateTime)
                .last("limit 1").one();
        LogicFlow logicFlow=LogicFlow.newInstance(workFlowVersion.getWorkFlow());
        List<LfNode> lfNodes=logicFlow.getNodes();
        List<INode> nodes=lfNodes.stream().filter(lfNode -> lfNode.getType().equals("base-node")).map(NodeFactory::getNode).toList();
        chatInfo.setNodes(nodes);
        chatInfo.setEdges(logicFlow.getEdges());
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


    public String chatMessage(ChatMessageDTO dto) {
        ChatInfo chatInfo = getChatInfo(dto.getChatId());
        if (chatInfo == null){
            dto.getSink().tryEmitError(new ApiException("会话不存在"));
            return "";
        }
        String appType=chatInfo.getApplication().getType();
        IChatActuator chatActuator= ChatActuatorBuilder.getActuator(appType);
        String answer = chatActuator.chatMessage(chatInfo,dto);
        dto.getSink().tryEmitComplete();
        return answer;
    }

    public IPage<ApplicationChatRecordVO> chatRecordPage(String chatId, int page, int size) {
        return chatRecordService.chatRecordPage(chatId, page, size);
    }

    public Boolean getChatRecordVote(String chatRecordId, ApplicationChatRecordEntity chatRecord) {
        chatRecord.setId(chatRecordId);
        return chatRecordService.updateById(chatRecord);
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
