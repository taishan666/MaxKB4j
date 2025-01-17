package com.tarzan.maxkb4j.module.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatImproveDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.*;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.application.vo.ApplicationChatRecordVO;
import com.tarzan.maxkb4j.module.application.vo.ApplicationPublicAccessClientStatisticsVO;
import com.tarzan.maxkb4j.module.application.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.chatpipeline.ChatCache;
import com.tarzan.maxkb4j.module.chatpipeline.ChatInfo;
import com.tarzan.maxkb4j.module.chatpipeline.PipelineManage;
import com.tarzan.maxkb4j.module.chatpipeline.handler.PostResponseHandler;
import com.tarzan.maxkb4j.module.chatpipeline.step.chatstep.impl.BaseChatStep;
import com.tarzan.maxkb4j.module.chatpipeline.step.generatehumanmessagestep.impl.GenerateHumanMessageStep;
import com.tarzan.maxkb4j.module.chatpipeline.step.resetproblemstep.impl.BaseResetProblemStep;
import com.tarzan.maxkb4j.module.chatpipeline.step.searchdatasetstep.impl.SearchDatasetStep;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.service.DatasetService;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.BeanUtil;
import com.tarzan.maxkb4j.util.JwtUtil;
import com.tarzan.maxkb4j.util.MD5Util;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Service
public class ApplicationService extends ServiceImpl<ApplicationMapper, ApplicationEntity> {

    @Autowired
    private ModelService modelService;
    @Autowired
    private ApplicationAccessTokenService accessTokenService;
    @Autowired
    private DatasetService datasetService;
    @Autowired
    private ApplicationChatService chatService;
    @Autowired
    private ApplicationWorkFlowVersionService workFlowVersionService;
    @Autowired
    private ApplicationDatasetMappingService applicationDatasetMappingService;
    @Autowired
    private ApplicationChatService applicationChatService;
    @Autowired
    private ApplicationPublicAccessClientService applicationPublicAccessClientService;
    @Autowired
    private ApplicationApiKeyService applicationApiKeyService;
    @Autowired
    private ApplicationChatRecordService chatRecordService;
    @Autowired
    private BaseChatStep baseChatStep;
    @Autowired
    private SearchDatasetStep searchDatasetStep;
    @Autowired
    private BaseResetProblemStep baseResetProblemStep;
    @Autowired
    private PostResponseHandler postResponseHandler;
    @Autowired
    private ApplicationAccessTokenService applicationAccessTokenService;

    public IPage<ApplicationEntity> selectAppPage(int page, int size, QueryDTO query) {
        Page<ApplicationEntity> appPage = new Page<>(page, size);
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(ApplicationEntity::getName, query.getName());
        }
        if (Objects.nonNull(query.getSelectUserId())) {
            wrapper.eq(ApplicationEntity::getUserId, query.getSelectUserId());
        }
        return this.page(appPage, wrapper);
    }

    public List<ModelEntity> getAppModels(UUID appId, String modelType) {
        modelType = StringUtils.isBlank(modelType) ? "LLM" : modelType;
        ApplicationEntity app = getById(appId);
        if (app == null) {
            return Collections.emptyList();
        }
        return modelService.getUserIdAndType(app.getUserId(), modelType);
    }


    public ApplicationAccessTokenEntity getAccessToken(UUID appId) {
        return accessTokenService.accessToken(appId);
    }

    public ApplicationAccessTokenEntity updateAccessToken(UUID appId, ApplicationAccessTokenEntity entity) {
        entity.setApplicationId(appId);
        if(entity.getAccessTokenReset()!=null&&entity.getAccessTokenReset()){
            //todo
            entity.setAccessToken("4142eb2fdb5b986e");
        }
        accessTokenService.updateById(entity);
        return accessTokenService.getById(appId);
    }

    public List<DatasetEntity> getDatasets(UUID appId) {
        ApplicationEntity app = getById(appId);
        if (app == null) {
            return Collections.emptyList();
        }
        return datasetService.getUserId(app.getUserId());
    }

    public IPage<ApplicationChatEntity> chatLogs(String appId, int page, int size, ChatQueryDTO query) {
        Page<ApplicationChatEntity> chatPage = new Page<>(page, size);
        return chatService.chatLogs(chatPage, UUID.fromString(appId), query);
    }

    public JSONArray modelParams(String appId, String modelId) {
        ModelEntity model = modelService.getById(UUID.fromString(modelId));
        if (model == null) {
            return new JSONArray();
        }
        return model.getModelParamsForm();
    }

    @Transactional
    public boolean deleteByAppId(UUID appId) {
        accessTokenService.remove(Wrappers.<ApplicationAccessTokenEntity>lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId));
        workFlowVersionService.remove(Wrappers.<ApplicationWorkFlowVersionEntity>lambdaQuery().eq(ApplicationWorkFlowVersionEntity::getApplicationId, appId));
        applicationDatasetMappingService.remove(Wrappers.<ApplicationDatasetMappingEntity>lambdaQuery().eq(ApplicationDatasetMappingEntity::getApplicationId, appId));
        return this.removeById(appId);
    }

    @Transactional
    public ApplicationEntity createApp(ApplicationEntity application) {
        String userId = StpUtil.getLoginIdAsString();
        application.setId(UUID.randomUUID());
        application.setUserId(UUID.fromString(userId));
        application.setIcon("");
        application.setWorkFlow(new JSONObject());
        application.setTtsModelParamsSetting(new JSONObject());
        application.setCleanTime(1000 * 365);
        application.setFileUploadEnable(false);
        application.setFileUploadSetting(new JSONObject());
        ApplicationAccessTokenEntity accessToken=new ApplicationAccessTokenEntity();
        accessToken.setApplicationId(application.getId());
        accessToken.setAccessNum(0);
        accessToken.setIsActive(true);
        accessToken.setShowSource(false);
        accessToken.setWhiteActive(false);
        accessToken.setWhiteList(new HashSet<>());
        accessToken.setAccessToken(MD5Util.encrypt(UUID.randomUUID().toString(),8,24));
        applicationAccessTokenService.save(accessToken);
        this.save(application);
        return application;
    }

    public boolean improveChatLogs(UUID appId, ChatImproveDTO dto) {
        return false;
    }

    public ApplicationVO getAppById(UUID appId) {
        ApplicationEntity entity = this.getById(appId);
        if (entity == null) {
            return null;
        }
        ApplicationVO vo = BeanUtil.copy(entity, ApplicationVO.class);
        List<UUID> datasetIds = new ArrayList<>();
        List<ApplicationDatasetMappingEntity> mappingEntities = applicationDatasetMappingService.lambdaQuery()
                .select(ApplicationDatasetMappingEntity::getDatasetId)
                .eq(ApplicationDatasetMappingEntity::getApplicationId, appId).list();
        if (!CollectionUtils.isEmpty(mappingEntities)) {
            datasetIds = mappingEntities.stream().map(ApplicationDatasetMappingEntity::getDatasetId).toList();
        }
        vo.setDatasetIdList(datasetIds);
        return vo;
    }

    // 定义日期格式
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public List<ApplicationStatisticsVO> statistics(UUID appId, ChatQueryDTO query) {
        List<ApplicationStatisticsVO> result = new ArrayList<>();
        List<ApplicationStatisticsVO> list = applicationChatService.statistics(appId, query);
        List<ApplicationPublicAccessClientStatisticsVO> AccessClientList = applicationPublicAccessClientService.statistics(appId, query);
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

    public List<ApplicationApiKeyEntity> listApikey(UUID appId) {
        return applicationApiKeyService.lambdaQuery().eq(ApplicationApiKeyEntity::getApplicationId, appId).list();
    }

    public boolean createApikey(UUID appId) {
        ApplicationApiKeyEntity entity = new ApplicationApiKeyEntity();
        entity.setApplicationId(appId);
        entity.setIsActive(true);
        entity.setAllowCrossDomain(false);
        String uuid = UUID.randomUUID().toString();
        entity.setSecretKey("maxKb4j-" + uuid.replaceAll("-", ""));
        entity.setUserId(UUID.fromString(StpUtil.getLoginIdAsString()));
        entity.setCrossDomainList(new HashSet<>());
        return applicationApiKeyService.save(entity);
    }

    public boolean updateApikey(UUID appId, UUID apikeyId, ApplicationApiKeyEntity entity) {
        entity.setId(apikeyId);
        return applicationApiKeyService.updateById(entity);
    }

    public boolean deleteApikey(UUID appId, UUID apikeyId) {
        return applicationApiKeyService.removeById(apikeyId);
    }

    public Flux<JSONObject> chatMessage(UUID chatId, JSONObject json, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Claims claims = JwtUtil.parseToken(authorization);
        String clientId = (String) claims.get("client_id");
        String clientType = (String) claims.get("type");
        ChatInfo chatInfo = getChatInfo(chatId);
        if(chatInfo==null){
            System.err.println("会话不存在");
        }
        if(!claims.isEmpty()){
            try {
                isValidApplication(chatInfo,UUID.fromString(clientId),clientType);
            } catch (Exception e) {
                JSONObject data = new JSONObject();
                data.put("content",e.getMessage());
                return Flux.just(data);
            }
        }
        boolean stream = json.getBoolean("stream") == null || json.getBoolean("stream");
        String problemText = json.getString("message");
        boolean reChat = json.getBooleanValue("rechat");
        List<UUID> excludeParagraphIds = new ArrayList<>();
        if (reChat) {
            UUID chatRecordId = json.getObject("chat_record_id", UUID.class);
            if (Objects.nonNull(chatRecordId)) {
                ApplicationChatRecordVO chatRecord = getChatRecordInfo(chatId, chatRecordId);
                List<ParagraphVO> paragraphs = chatRecord.getParagraphList();
                if (!CollectionUtils.isEmpty(paragraphs)) {
                    excludeParagraphIds = paragraphs.stream().map(ParagraphVO::getId).toList();
                }
            }
        }
        ApplicationEntity application = chatInfo.getApplication();
        PipelineManage.Builder pipelineManageBuilder = new PipelineManage.Builder();
        if (application.getProblemOptimization()) {
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

    public void isValidApplication(ChatInfo chatInfo,UUID clientId,String clientType) throws Exception {
        isValidIntraDayAccessNum(chatInfo.getApplication().getId(),clientId,clientType);
        UUID modelId=chatInfo.getApplication().getModelId();
        ModelEntity model=modelService.getById(modelId);
        if(Objects.isNull(model)||!"SUCCESS".equals(model.getStatus())) {
            throw new Exception("当前模型不可用");
        }
    }

    public void isValidIntraDayAccessNum(UUID appId,UUID clientId,String clientType) throws Exception {
        if("APPLICATION_ACCESS_TOKEN".equals(clientType)){
            ApplicationPublicAccessClientEntity   accessClient=applicationPublicAccessClientService.getById(clientId);
            if (Objects.isNull(accessClient)) {
                accessClient=new ApplicationPublicAccessClientEntity();
                accessClient.setId(clientId);
                accessClient.setApplicationId(appId);
                accessClient.setAccessNum(0);
                accessClient.setIntradayAccessNum(0);
                applicationPublicAccessClientService.save(accessClient);
            }
            ApplicationAccessTokenEntity  appAccessToken = applicationAccessTokenService.lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId).one();
            if(appAccessToken.getAccessNum()<accessClient.getIntradayAccessNum()){
                throw new Exception("访问次数超过今日访问量");
            }
        }
    }

    public UUID chatOpenTest(ApplicationEntity application) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(UUID.randomUUID());
        application.setId(null);
        chatInfo.setApplication(application);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    public UUID chatOpen(UUID appId) {
        ChatInfo chatInfo = new ChatInfo();
        chatInfo.setChatId(UUID.randomUUID());
        ApplicationEntity application = this.getById(appId);
        List<ApplicationDatasetMappingEntity> list = applicationDatasetMappingService.lambdaQuery().eq(ApplicationDatasetMappingEntity::getApplicationId, application.getId()).list();
        application.setDatasetIdList(list.stream().map(ApplicationDatasetMappingEntity::getDatasetId).toList());
        chatInfo.setApplication(application);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo.getChatId();
    }

    private ChatInfo getChatInfo(UUID chatId) {
        ChatInfo chatInfo = ChatCache.get(chatId);
        if (chatInfo == null) {
            return reChatOpen(chatId);
        }
        return chatInfo;
    }

    public ChatInfo reChatOpen(UUID chatId) {
        ChatInfo chatInfo = new ChatInfo();
        ApplicationChatEntity chatEntity = chatService.getById(chatId);
        chatInfo.setChatId(chatId);
        ApplicationEntity application = this.getById(chatEntity.getApplicationId());
        List<ApplicationDatasetMappingEntity> list = applicationDatasetMappingService.lambdaQuery().eq(ApplicationDatasetMappingEntity::getApplicationId, application.getId()).list();
        application.setDatasetIdList(list.stream().map(ApplicationDatasetMappingEntity::getDatasetId).toList());
        chatInfo.setApplication(application);
        ChatCache.put(chatInfo.getChatId(), chatInfo);
        return chatInfo;
    }

    public ApplicationChatRecordVO getChatRecordInfo(UUID chatId, UUID chatRecordId) {
        return chatRecordService.getChatRecordInfo(chatId, chatRecordId);
    }

    public String authentication(HttpServletRequest request, JSONObject params) throws Exception {
        String token = request.getHeader("Authorization");
        Map<String, Object> tokenDetails = null;

        try {
            // 校验token
            if (token != null && !token.isEmpty()) {
                tokenDetails = JwtUtil.parseToken(token); // 假设parseToken方法用于解析token
            }
        } catch (Exception e) {
            token = null;
        }

    /*    if (withValid) {
            this.isValid(); // 假设有此方法来校验有效性
        }*/

        String accessToken = params.getString("access_token"); // 假设getData()返回一个Map
        ApplicationAccessTokenEntity applicationAccessToken = applicationAccessTokenService.lambdaQuery().eq(ApplicationAccessTokenEntity::getAccessToken, accessToken).one();
        if (applicationAccessToken != null && applicationAccessToken.getIsActive()) {
            ApplicationEntity application = this.lambdaQuery().select(ApplicationEntity::getUserId).eq(ApplicationEntity::getId, applicationAccessToken.getApplicationId()).one();
            Map<String, Object> authentication = new HashMap<>();
            JSONObject authenticationValue = params.getJSONObject("authentication_value");
            String clientId;
            if (tokenDetails != null && tokenDetails.containsKey("client_id")) {
                clientId = (String) tokenDetails.get("client_id");
                authentication = (Map<String, Object>) tokenDetails.get("authentication");
            } else {
                clientId = UUID.randomUUID().toString();
            }

            if (authenticationValue != null) {
                // 认证用户token
                // authAuthenticationValue(authenticationValue, applicationAccessToken.getApplicationId());
                authentication.put("type", authenticationValue.get("type"));
                authentication.put("value", passwordEncrypt(authenticationValue.getString("value"))); // 假设passwordEncrypt方法存在
            }

            Map<String, Object> data = new HashMap<>();
            data.put("application_id", applicationAccessToken.getApplicationId());
            data.put("user_id", application.getUserId());
            data.put("access_token", applicationAccessToken.getAccessToken());
            data.put("type", "APPLICATION_ACCESS_TOKEN");
            data.put("client_id", clientId);
            data.put("authentication", authentication);

            return JwtUtil.createToken(data);
        } else {
            log.error("404");
            throw new Exception("404");
        }
    }

/*    public void authAuthenticationValue(String authenticationValue, String applicationId) throws Exception {
        ApplicationSettingModel applicationSettingModel = DBModelManage.getModel("application_setting");
        XpackCache xpackCache = DBModelManage.getModel("xpack_cache");
        boolean X_PACK_LICENSE_IS_VALID = (xpackCache != null) && Boolean.TRUE.equals(xpackCache.get("XPACK_LICENSE_IS_VALID"));

        if (applicationSettingModel != null && X_PACK_LICENSE_IS_VALID) {
            ApplicationSetting applicationSetting = findApplicationSetting(applicationSettingModel, applicationId);
            if (applicationSetting != null && applicationSetting.getAuthentication() != null && authenticationValue != null) {
                Map<String, Object> authValueMap = parseAuthenticationValue(authenticationValue); // 假设存在解析authenticationValue的方法

                if ("password".equals(authValueMap.get("type"))) {
                    if (!authPassword(authValueMap, applicationSetting.getAuthenticationValue())) {
                        throw new AppApiException(1005, "密码错误");
                    }
                }
            }
        }
    }*/

    private String passwordEncrypt(String text) {
        return text;
    }

    public JSONObject appProfile(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Claims claims = JwtUtil.parseToken(authorization);
        String appId = (String) claims.get("application_id");
        ApplicationEntity application=this.getById(UUID.fromString(appId));
        ApplicationAccessTokenEntity appAccessToken=applicationAccessTokenService.getById(UUID.fromString(appId));
        JSONObject result=new JSONObject();
        result.put("id",appId);
        result.put("type",application.getType());
        result.put("name",application.getName());
        result.put("desc",application.getDesc());
        result.put("icon",application.getIcon());
        result.put("prologue",application.getPrologue());
        result.put("dialogue_number",application.getDialogueNumber());
        result.put("multiple_rounds_dialogue",application.getDialogueNumber()>0);
        result.put("file_upload_enable",application.getFileUploadEnable());
        result.put("file_upload_setting",application.getFileUploadSetting());
        result.put("stt_model_enable",application.getSttModelEnable());
        result.put("stt_model_id",application.getSttModelId());
        result.put("tts_model_enable",application.getSttModelEnable());
        result.put("tts_type",application.getTtsType());
        result.put("tts_model_id",application.getTtsModelId());
        result.put("work_flow",application.getWorkFlow());
        result.put("show_source",appAccessToken.getShowSource());
        return result;
    }

    public IPage<ApplicationChatEntity> clientChatPage(UUID appId, int page, int size, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        Claims claims = JwtUtil.parseToken(authorization);
        String clientId = (String) claims.get("client_id");
        return chatService.clientChatPage(appId, UUID.fromString(clientId), page, size);
    }

    public IPage<ApplicationChatRecordVO> chatRecordPage(UUID chatId, int page, int size) {
        return chatRecordService.chatRecordPage(chatId, page, size);
    }

    public Boolean getChatRecordVote(UUID chatRecordId, ApplicationChatRecordEntity chatRecord) {
        chatRecord.setId(chatRecordId);
        return chatRecordService.updateById(chatRecord);
    }

    public List<ParagraphVO> hitTest(UUID id, HitTestDTO dto) {
        List<ApplicationDatasetMappingEntity> mapping=applicationDatasetMappingService.lambdaQuery()
                .select(ApplicationDatasetMappingEntity::getDatasetId)
                .eq(ApplicationDatasetMappingEntity::getApplicationId,id).list();
        if(CollectionUtils.isEmpty(mapping)) {
            return Collections.emptyList();
        }
        List<UUID> datasetIds=mapping.stream().map(ApplicationDatasetMappingEntity::getDatasetId).toList();
        return datasetService.hitTest(datasetIds,dto);
    }

    public boolean copyApp(int type) {
        return true;
    }
}
