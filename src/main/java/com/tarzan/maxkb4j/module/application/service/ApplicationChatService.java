package com.tarzan.maxkb4j.module.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.cache.ChatCache;
import com.tarzan.maxkb4j.module.application.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.application.chatpipeline.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.application.chatpipeline.step.chatstep.impl.BaseChatStep;
import com.tarzan.maxkb4j.module.application.chatpipeline.step.generatehumanmessagestep.impl.GenerateHumanMessageStep;
import com.tarzan.maxkb4j.module.application.chatpipeline.step.resetproblemstep.impl.BaseResetProblemStep;
import com.tarzan.maxkb4j.module.application.chatpipeline.step.searchdatasetstep.impl.SearchDatasetStep;
import com.tarzan.maxkb4j.module.application.dto.ChatInfo;
import com.tarzan.maxkb4j.module.application.dto.ChatMessageDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.*;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.vo.ApplicationPublicAccessClientStatisticsVO;
import com.tarzan.maxkb4j.module.application.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.workflow.Flow;
import com.tarzan.maxkb4j.module.application.workflow.WorkflowManage;
import com.tarzan.maxkb4j.module.application.workflow.dto.FlowParams;
import com.tarzan.maxkb4j.module.application.workflow.dto.SystemToResponse;
import com.tarzan.maxkb4j.module.application.workflow.handler.WorkFlowPostHandler;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.file.service.FileService;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author tarzan
 * @date 2024-12-26 09:50:23
 */
@Service
public class ApplicationChatService extends ServiceImpl<ApplicationChatMapper, ApplicationChatEntity>{

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationDatasetMappingService datasetMappingService;
    @Autowired
    private ApplicationWorkFlowVersionService workFlowVersionService;
    @Autowired
    private ApplicationPublicAccessClientService publicAccessClientService;
    @Autowired
    private ApplicationAccessTokenService accessTokenService;
    @Autowired
    private ApplicationChatRecordService chatRecordService;
    @Autowired
    private ModelService modelService;
    @Autowired
    private FileService fileService;
    @Autowired
    private BaseChatStep baseChatStep;
    @Autowired
    private SearchDatasetStep searchDatasetStep;
    @Autowired
    private BaseResetProblemStep baseResetProblemStep;
    @Autowired
    private PostResponseHandler postResponseHandler;


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
        application.setType("WORKFLOW");
        application.setUserId(StpUtil.getLoginIdAsString());
        chatInfo.setApplication(application);
        chatInfo.setWorkFlowVersion(workflowVersion);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    public String chatOpen(String appId) {
        ApplicationEntity application = applicationService.getById(appId);
        if ("SIMPLE".equals(application.getType())) {
            return chatOpenSimple(application);
        } else {
            return chatOpenWorkflow(application);
        }
    }

    public String chatOpenSimple(ApplicationEntity application) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(IdWorker.get32UUID());
        List<ApplicationDatasetMappingEntity> list = datasetMappingService.lambdaQuery().eq(ApplicationDatasetMappingEntity::getApplicationId, application.getId()).list();
        application.setDatasetIdList(list.stream().map(ApplicationDatasetMappingEntity::getDatasetId).toList());
        chatInfo.setApplication(application);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    public String chatOpenWorkflow(ApplicationEntity application) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(IdWorker.get32UUID());
        ApplicationWorkFlowVersionEntity workFlowVersion = workFlowVersionService.lambdaQuery()
                .eq(ApplicationWorkFlowVersionEntity::getApplicationId, application.getId())
                .orderByDesc(ApplicationWorkFlowVersionEntity::getCreateTime)
                .last("limit 1").one();
        chatInfo.setApplication(application);
        chatInfo.setWorkFlowVersion(workFlowVersion);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    public ChatInfo reChatOpen(String chatId) {
        ChatInfo chatInfo = new ChatInfo();
        ApplicationChatEntity chatEntity = this.getById(chatId);
        chatInfo.setChatId(chatId);
        ApplicationEntity application = applicationService.getById(chatEntity.getApplicationId());
        List<ApplicationDatasetMappingEntity> list = datasetMappingService.lambdaQuery().eq(ApplicationDatasetMappingEntity::getApplicationId, application.getId()).list();
        application.setDatasetIdList(list.stream().map(ApplicationDatasetMappingEntity::getDatasetId).toList());
        chatInfo.setApplication(application);
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

    public ApplicationChatRecordVO getChatRecordInfo(String chatId, String chatRecordId) {
        return chatRecordService.getChatRecordInfo(chatId, chatRecordId);
    }

    public Flux<JSONObject> chatMessage(String chatId, ChatMessageDTO dto, HttpServletRequest request) {
        ChatInfo chatInfo = getChatInfo(chatId);
        if (chatInfo.getApplication().getType().equals("SIMPLE")) {
            return chatSimple(chatId, dto, request);
        } else {
            return chatWorkflow(chatId, dto, request);
        }
    }

    public Flux<JSONObject> chatSimple(String chatId, ChatMessageDTO dto, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Claims claims = JwtUtil.parseToken(authorization);
        String clientId = (String) claims.get("client_id");
        String clientType = (String) claims.get("type");
        ChatInfo chatInfo = getChatInfo(chatId);
        if (chatInfo == null) {
            System.err.println("会话不存在");
        } else {
            if (!claims.isEmpty()) {
                try {
                    isValidApplication(chatInfo, clientId, clientType);
                } catch (Exception e) {
                    log.error("会话不存在", e);
                    JSONObject data = new JSONObject();
                    data.put("content", "系统错误！");
                    return Flux.just(data);
                }
            }
        }

        boolean stream = dto.getStream() == null || dto.getStream();
        String problemText = dto.getMessage();
        boolean reChat = dto.getReChat();
        List<String> excludeParagraphIds = new ArrayList<>();
        if (reChat) {
            String chatRecordId = dto.getChatRecordId();
            if (Objects.nonNull(chatRecordId)) {
                ApplicationChatRecordVO chatRecord = getChatRecordInfo(chatId, chatRecordId);
                List<ParagraphVO> paragraphs = chatRecord.getParagraphList();
                if (!CollectionUtils.isEmpty(paragraphs)) {
                    excludeParagraphIds = paragraphs.stream().map(ParagraphVO::getId).toList();
                }
            }
        }
        assert chatInfo != null;
        ApplicationEntity application = chatInfo.getApplication();
        PipelineManage.Builder pipelineManageBuilder = new PipelineManage.Builder();
        Boolean problemOptimization = application.getProblemOptimization();
        if (Objects.nonNull(problemOptimization) && problemOptimization) {
            pipelineManageBuilder.addStep(baseResetProblemStep);
        }
        pipelineManageBuilder.addStep(searchDatasetStep);
        pipelineManageBuilder.addStep(GenerateHumanMessageStep.class);
        pipelineManageBuilder.addStep(baseChatStep);
        PipelineManage pipelineManage = pipelineManageBuilder.build();

        Map<String, Object> params = chatInfo.toPipelineManageParams(problemText, postResponseHandler, excludeParagraphIds, clientId, clientType, stream);
        pipelineManage.run(params);
        return pipelineManage.response;
    }

    public Flux<JSONObject> chatWorkflow(String chatId, ChatMessageDTO dto, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Claims claims = JwtUtil.parseToken(authorization);
        String clientId = (String) claims.get("client_id");
        String clientType = (String) claims.get("type");
        ChatInfo chatInfo = getChatInfo(chatId);
        if (chatInfo == null) {
            System.err.println("会话不存在");
        } else {
            if (!claims.isEmpty()) {
                try {
                    isValidIntraDayAccessNum(chatInfo.getApplication().getId(), clientId, clientType);
                } catch (Exception e) {
                    JSONObject data = new JSONObject();
                    data.put("content", e.getMessage());
                    return Flux.just(data);
                }
            }
        }
        boolean reChat = dto.getReChat();
        ApplicationChatRecordVO chatRecord = null;
        if (reChat) {
            String chatRecordId = dto.getChatRecordId();
            if (Objects.nonNull(chatRecordId)) {
                chatRecord = getChatRecordInfo(chatId, chatRecordId);
            }
        }
        assert chatInfo != null;
        FlowParams flowParams = new FlowParams();
        flowParams.setChatId(chatInfo.getChatId());
        flowParams.setChatRecordId(dto.getChatRecordId() == null ? IdWorker.get32UUID() : dto.getChatRecordId());
        flowParams.setQuestion(dto.getMessage());
        flowParams.setReChat(dto.getReChat());
        flowParams.setUserId(StpUtil.getLoginIdAsString());
        flowParams.setClientId(clientId);
        flowParams.setClientType(clientType);
        flowParams.setStream(dto.getStream() == null || dto.getStream());
        flowParams.setHistoryChatRecord(chatInfo.getChatRecordList());
        WorkflowManage workflowManage = new WorkflowManage(Flow.newInstance(chatInfo.getWorkFlowVersion().getWorkFlow()),
                flowParams,
                new WorkFlowPostHandler(chatInfo, clientId, clientType),
                new SystemToResponse(),
                dto.getFormData(),
                dto.getImageList(),
                dto.getDocumentList(),
                dto.getAudioList(),
                null,
                null,
                chatRecord,
                null);
        return workflowManage.run();
    }

    public void isValidIntraDayAccessNum(String appId, String clientId, String clientType) throws Exception {
        if ("APPLICATION_ACCESS_TOKEN".equals(clientType)) {
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
                throw new Exception("访问次数超过今日访问量");
            }
        }
    }

    public void isValidApplication(ChatInfo chatInfo, String clientId, String clientType) throws Exception {
        isValidIntraDayAccessNum(chatInfo.getApplication().getId(), clientId, clientType);
        String modelId = chatInfo.getApplication().getModelId();
        ModelEntity model = modelService.getById(modelId);
        if (Objects.isNull(model) || !"SUCCESS".equals(model.getStatus())) {
            throw new Exception("当前模型不可用");
        }
    }

    public IPage<ApplicationChatRecordVO> chatRecordPage(String chatId, int page, int size) {
        return chatRecordService.chatRecordPage(chatId, page, size);
    }

    public Boolean getChatRecordVote(String chatRecordId, ApplicationChatRecordEntity chatRecord) {
        chatRecord.setId(chatRecordId);
        return chatRecordService.updateById(chatRecord);
    }


    public List<JSONObject> uploadFile(String id, String chatId, MultipartFile[] files) {
        List<JSONObject> fileList = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                fileList.add(fileService.uploadFile(file.getOriginalFilename(),file.getBytes()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return fileList;
    }


    // 定义日期格式
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<ApplicationStatisticsVO> statistics(String appId, ChatQueryDTO query) {
        List<ApplicationStatisticsVO> result = new ArrayList<>();
        List<ApplicationStatisticsVO> list = baseMapper.statistics(appId, query);
        List<ApplicationPublicAccessClientStatisticsVO> AccessClientList = publicAccessClientService.statistics(appId, query);
        // 将字符串解析为LocalDate对象
        LocalDate startDate = LocalDate.parse(query.getStartTime(), formatter);
        LocalDate endDate = LocalDate.parse(query.getEndTime(), formatter);
        // 遍历从开始日期到结束日期之间的所有日期
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String day = date.format(formatter);
            ApplicationStatisticsVO vo = getApplicationStatisticsVO(list, day);
            ApplicationPublicAccessClientStatisticsVO accessClientStatisticsVO = getApplicationPublicAccessClientStatisticsVO(AccessClientList, day);
            if (accessClientStatisticsVO != null) {
                vo.setCustomerAddedCount(accessClientStatisticsVO.getCustomerAddedCount());
            }
            result.add(vo);
        }
        return result;
    }

    public ApplicationStatisticsVO getApplicationStatisticsVO(List<ApplicationStatisticsVO> list, String day) {
        if (!CollectionUtils.isEmpty(list)) {
            Optional<ApplicationStatisticsVO> optional = list.stream().filter(e -> e.getDay().equals(day)).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        ApplicationStatisticsVO vo = new ApplicationStatisticsVO();
        vo.setDay(day);
        vo.setStarNum(0);
        vo.setTokensNum(0);
        vo.setCustomerNum(0);
        vo.setChatRecordCount(0);
        vo.setTrampleNum(0);
        return vo;
    }

    public ApplicationPublicAccessClientStatisticsVO getApplicationPublicAccessClientStatisticsVO(List<ApplicationPublicAccessClientStatisticsVO> list, String day) {
        if (!CollectionUtils.isEmpty(list)) {
            Optional<ApplicationPublicAccessClientStatisticsVO> optional = list.stream().filter(e -> e.getDay().equals(day)).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        return null;
    }
}
