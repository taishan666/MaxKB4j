package com.tarzan.maxkb4j.module.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.core.exception.ApiException;
import com.tarzan.maxkb4j.core.workflow.WorkflowManage;
import com.tarzan.maxkb4j.core.workflow.dto.ChatFile;
import com.tarzan.maxkb4j.core.workflow.logic.LogicFlow;
import com.tarzan.maxkb4j.core.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.ragpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.chatstep.impl.BaseChatStep;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.resetproblemstep.impl.BaseResetProblemStep;
import com.tarzan.maxkb4j.module.application.ragpipeline.step.searchdatasetstep.impl.SearchDatasetStep;
import com.tarzan.maxkb4j.module.application.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.*;
import com.tarzan.maxkb4j.module.application.enums.AppType;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import com.tarzan.maxkb4j.module.application.vo.ChatRecordDetailVO;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.resource.service.MongoFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final ApplicationPublicAccessClientService publicAccessClientService;
    private final ApplicationAccessTokenService accessTokenService;
    private final ApplicationChatRecordService chatRecordService;
    private final ModelService modelService;
   // private final FileService fileService;
    private final MongoFileService fileService;
    private final BaseChatStep baseChatStep;
    private final SearchDatasetStep searchDatasetStep;
    private final BaseResetProblemStep baseResetProblemStep;
    private final PostResponseHandler postResponseHandler;


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
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(IdWorker.get32UUID());
        application.setId(null);
        chatInfo.setApplication(application);
        application.setType(AppType.SIMPLE.name());
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    public String chatWorkflowOpenTest(ApplicationEntity application) {
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
    }

    public String chatOpen(String appId) {
       return chatOpen(appId,null);
    }

    public String chatOpen(String appId,String chatId) {
        ApplicationEntity application = applicationMapper.selectById(appId);
        if (StringUtils.isBlank(chatId)){
            chatId=IdWorker.get32UUID();
        }
        if (AppType.SIMPLE.name().equals(application.getType())) {
            return chatOpenSimple(application,chatId);
        } else {
            return chatOpenWorkflow(application,chatId);
        }
    }

    public String chatOpenSimple(ApplicationEntity application,String chatId) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(chatId);
        List<ApplicationDatasetMappingEntity> list = datasetMappingService.lambdaQuery().eq(ApplicationDatasetMappingEntity::getApplicationId, application.getId()).list();
        application.setDatasetIdList(list.stream().map(ApplicationDatasetMappingEntity::getDatasetId).toList());
        chatInfo.setApplication(application);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatId;
    }

    public String chatOpenWorkflow(ApplicationEntity application,String chatId) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(chatId);
        ApplicationWorkFlowVersionEntity workFlowVersion = workFlowVersionService.lambdaQuery()
                .eq(ApplicationWorkFlowVersionEntity::getApplicationId, application.getId())
                .orderByDesc(ApplicationWorkFlowVersionEntity::getCreateTime)
                .last("limit 1").one();
        chatInfo.setApplication(application);
        chatInfo.setWorkFlowVersion(workFlowVersion);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatId;
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
        chatInfo.setWorkFlowVersion(workFlowVersion);
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


    public Flux<ChatMessageVO> chatMessage(String chatId, ChatMessageDTO dto) {
        //todo 验证(当作节点时，clientId获取问题)
        /*String clientId = (String) StpUtil.getExtra("client_id");
        String clientType = (String) StpUtil.getExtra("client_type");*/
        ChatInfo chatInfo = getChatInfo(chatId);
        try {
            isValidApplication(chatInfo, dto.getClientId(), dto.getClientType());
        } catch (Exception e) {
            return Flux.just(new ChatMessageVO(chatId,  e.getMessage(), true));
        }
        if (chatInfo.getApplication().getType().equals("SIMPLE")) {
            return chatSimple(chatInfo, dto);
        } else {
            return chatWorkflow(chatInfo, dto);
        }
    }

    public Flux<ChatMessageVO> chatSimple(ChatInfo chatInfo, ChatMessageDTO dto) {
        String modelId = chatInfo.getApplication().getModelId();
        ModelEntity model = modelService.getById(modelId);
        if (Objects.isNull(model) || !"SUCCESS".equals(model.getStatus())) {
            throw new ApiException("当前模型不可用");
        }
        boolean stream = dto.getStream() == null || dto.getStream();
        String problemText = dto.getMessage();
        boolean reChat = dto.getReChat();
        List<String> excludeParagraphIds = new ArrayList<>();
        if (reChat) {
            String chatRecordId = dto.getChatRecordId();
            if (Objects.nonNull(chatRecordId)) {
                ApplicationChatRecordVO chatRecord = chatRecordService.getChatRecordInfo(chatInfo, chatRecordId);
                List<ParagraphVO> paragraphs = chatRecord.getParagraphList();
                if (!CollectionUtils.isEmpty(paragraphs)) {
                    excludeParagraphIds = paragraphs.stream().map(ParagraphVO::getId).toList();
                }
            }
        }
        ApplicationEntity application = chatInfo.getApplication();
        PipelineManage.Builder pipelineManageBuilder = new PipelineManage.Builder();
        Boolean problemOptimization = application.getProblemOptimization();
        if (!CollectionUtils.isEmpty(application.getDatasetIdList())) {
            if (Objects.nonNull(problemOptimization) && problemOptimization) {
                pipelineManageBuilder.addStep(baseResetProblemStep);
            }
            pipelineManageBuilder.addStep(searchDatasetStep);
        }
        pipelineManageBuilder.addStep(baseChatStep);
        PipelineManage pipelineManage = pipelineManageBuilder.build();

        Map<String, Object> params = chatInfo.toPipelineManageParams(problemText, postResponseHandler, excludeParagraphIds, dto.getClientId(), dto.getClientType(), stream);
        pipelineManage.run(params);
        return pipelineManage.response;
    }

    public Flux<ChatMessageVO> chatWorkflow(ChatInfo chatInfo, ChatMessageDTO dto) {
        ApplicationChatRecordVO chatRecord = null;
        String chatRecordId = dto.getChatRecordId();
        if(StringUtils.isNotBlank(chatRecordId)){
            chatRecord = chatRecordService.getChatRecordInfo(chatInfo, chatRecordId);
        }
        FlowParams flowParams = new FlowParams();
        flowParams.setChatId(chatInfo.getChatId());
        flowParams.setChatRecordId(dto.getChatRecordId() == null ? IdWorker.get32UUID() : dto.getChatRecordId());
        flowParams.setQuestion(dto.getMessage());
        flowParams.setReChat(dto.getReChat());
        flowParams.setUserId(StpUtil.getLoginIdAsString());
        flowParams.setClientId(dto.getClientId());
        flowParams.setClientType(dto.getClientType());
        flowParams.setStream(dto.getStream() == null || dto.getStream());
        flowParams.setHistoryChatRecord(chatInfo.getChatRecordList());
        WorkflowManage workflowManage = new WorkflowManage(LogicFlow.newInstance(chatInfo.getWorkFlowVersion().getWorkFlow()),
                flowParams,
                postResponseHandler,
                dto.getFormData(),
                dto.getImageList(),
                dto.getDocumentList(),
                dto.getAudioList(),
                dto.getRuntimeNodeId(),
                dto.getNodeData(),
                chatRecord,
                null);
        return workflowManage.run();
    }

    public void isValidIntraDayAccessNum(String appId, String clientId, String clientType) {
        if (AuthType.ACCESS_TOKEN.name().equals(clientType)) {
            if(Objects.nonNull(appId)){
                ApplicationPublicAccessClientEntity accessClient = publicAccessClientService.getById(clientId);
                if (Objects.isNull(accessClient)) {
                    accessClient = new ApplicationPublicAccessClientEntity();
                    accessClient.setId(clientId);
                    accessClient.setApplicationId(appId);
                    accessClient.setAccessNum(0);
                    accessClient.setIntraDayAccessNum(0);
                    publicAccessClientService.save(accessClient);
                }
                ApplicationAccessTokenEntity appAccessToken = accessTokenService.lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId).one();
                if (appAccessToken.getAccessNum() < accessClient.getIntraDayAccessNum()) {
                    throw new ApiException("访问次数超过今日访问量");
                }
            }
        }
    }

    public void isValidApplication(ChatInfo chatInfo, String clientId, String clientType) {
        if (chatInfo == null) {
            throw new ApiException("会话不存在");
        }
        isValidIntraDayAccessNum(chatInfo.getApplication().getId(), clientId, clientType);
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
