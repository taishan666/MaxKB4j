package com.tarzan.maxkb4j.module.application.service;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.core.common.dto.QueryDTO;
import com.tarzan.maxkb4j.core.exception.AccessException;
import com.tarzan.maxkb4j.core.exception.ApiException;
import com.tarzan.maxkb4j.module.application.dto.ApplicationAccessTokenDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatImproveDTO;
import com.tarzan.maxkb4j.module.application.dto.EmbedDTO;
import com.tarzan.maxkb4j.module.application.dto.MaxKb4J;
import com.tarzan.maxkb4j.module.application.entity.*;
import com.tarzan.maxkb4j.module.application.enums.AuthType;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.application.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.vo.McpToolVO;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.entity.ParagraphEntity;
import com.tarzan.maxkb4j.module.dataset.service.DatasetService;
import com.tarzan.maxkb4j.module.dataset.service.ParagraphService;
import com.tarzan.maxkb4j.module.dataset.service.RetrieveService;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.functionlib.entity.FunctionLibEntity;
import com.tarzan.maxkb4j.module.functionlib.service.FunctionLibService;
import com.tarzan.maxkb4j.module.mcplib.entity.McpLibEntity;
import com.tarzan.maxkb4j.module.mcplib.service.McpLibService;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.info.service.ModelService;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseSpeechToText;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseTextToSpeech;
import com.tarzan.maxkb4j.module.resource.service.ImageService;
import com.tarzan.maxkb4j.module.system.team.service.TeamMemberPermissionService;
import com.tarzan.maxkb4j.module.system.user.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
import com.tarzan.maxkb4j.util.*;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.request.json.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Service
@AllArgsConstructor
public class ApplicationService extends ServiceImpl<ApplicationMapper, ApplicationEntity> {

    private final ModelService modelService;
    private final DatasetService datasetService;
    private final McpLibService  mcpLibService;
    private final ImageService imageService;
    private final UserService userService;
    private final RetrieveService retrieveService;
    private final ParagraphService paragraphService;
    private final TeamMemberPermissionService memberPermissionService;
    private final ApplicationAccessTokenService accessTokenService;
    private final ApplicationPlatformService platformService;
    private final ApplicationApiKeyService applicationApiKeyService;
    private final ApplicationPublicAccessClientService accessClientService;
    private final ApplicationWorkFlowVersionService workFlowVersionService;
    private final ApplicationDatasetMappingService datasetMappingService;
    private final ApplicationMcpMappingService mcpMappingService;
    private final ApplicationFunctionMappingService functionMappingService;
    private final ApplicationChatRecordService applicationChatRecordService;
    private final ApplicationChatService applicationChatService;
    private final FunctionLibService functionLibService;

    public IPage<ApplicationEntity> selectAppPage(int page, int size, QueryDTO query) {
        String loginId = StpUtil.getLoginIdAsString();
        Page<ApplicationEntity> appPage = new Page<>(page, size);
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(ApplicationEntity::getName, query.getName());
        }
        if (Objects.nonNull(query.getSelectUserId())) {
            wrapper.eq(ApplicationEntity::getUserId, query.getSelectUserId());
        }
        wrapper.eq(ApplicationEntity::getUserId, loginId);
        List<String> useTargetIds = memberPermissionService.getUseTargets("APPLICATION", loginId);
        if (!CollectionUtils.isEmpty(useTargetIds)) {
            wrapper.or().in(ApplicationEntity::getId, useTargetIds);
        }
        wrapper.orderByDesc(ApplicationEntity::getCreateTime);
        return this.page(appPage, wrapper);
    }

    public List<ModelEntity> getAppModels(String appId, String modelType) {
        modelType = StringUtils.isBlank(modelType) ? "LLM" : modelType;
        ApplicationEntity app = getById(appId);
        if (app == null) {
            return Collections.emptyList();
        }
        return modelService.getUserIdAndType(app.getUserId(), modelType);
    }


    public ApplicationAccessTokenEntity getAccessToken(String appId) {
        return accessTokenService.accessToken(appId);
    }

    public ApplicationAccessTokenEntity updateAccessToken(String appId, ApplicationAccessTokenDTO dto) {
        dto.setApplicationId(appId);
        if (dto.getAccessTokenReset() != null && dto.getAccessTokenReset()) {
            dto.setAccessToken(MD5Util.encrypt(UUID.randomUUID().toString(), 8, 24));
        }
        accessTokenService.updateById(BeanUtil.copy(dto, ApplicationAccessTokenEntity.class));
        return accessTokenService.getById(appId);
    }

    public List<DatasetEntity> getDataset(String appId) {
        ApplicationEntity app = getById(appId);
        if (app == null) {
            return Collections.emptyList();
        }
        return datasetService.getUserId(app.getUserId());
    }

    public List<McpLibEntity> getMcp(String appId) {
        ApplicationEntity app = getById(appId);
        if (app == null) {
            return Collections.emptyList();
        }
        return mcpLibService.getUserId(app.getUserId());
    }


    public JSONArray modelParams(String appId, String modelId) {
        ModelEntity model = modelService.getById(modelId);
        if (model == null) {
            return new JSONArray();
        }
        return model.getModelParamsForm();
    }

    @Transactional
    public boolean deleteByAppId(String appId) {
        accessTokenService.remove(Wrappers.<ApplicationAccessTokenEntity>lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId));
        platformService.remove(Wrappers.<ApplicationPlatformEntity>lambdaQuery().eq(ApplicationPlatformEntity::getApplicationId, appId));
        applicationApiKeyService.remove(Wrappers.<ApplicationApiKeyEntity>lambdaQuery().eq(ApplicationApiKeyEntity::getApplicationId, appId));
        accessClientService.remove(Wrappers.<ApplicationPublicAccessClientEntity>lambdaQuery().eq(ApplicationPublicAccessClientEntity::getApplicationId, appId));
        workFlowVersionService.remove(Wrappers.<ApplicationWorkFlowVersionEntity>lambdaQuery().eq(ApplicationWorkFlowVersionEntity::getApplicationId, appId));
        datasetMappingService.remove(Wrappers.<ApplicationDatasetMappingEntity>lambdaQuery().eq(ApplicationDatasetMappingEntity::getApplicationId, appId));
        mcpMappingService.remove(Wrappers.<ApplicationMcpMappingEntity>lambdaQuery().eq(ApplicationMcpMappingEntity::getApplicationId, appId));
        functionMappingService.remove(Wrappers.<ApplicationFunctionMappingEntity>lambdaQuery().eq(ApplicationFunctionMappingEntity::getApplicationId, appId));
        List<String> chatIds = applicationChatService.list(Wrappers.<ApplicationChatEntity>lambdaQuery().eq(ApplicationChatEntity::getApplicationId, appId)).stream().map(ApplicationChatEntity::getId).toList();
        if (!CollectionUtils.isEmpty(chatIds)) {
            applicationChatService.remove(Wrappers.<ApplicationChatEntity>lambdaQuery().eq(ApplicationChatEntity::getApplicationId, appId));
            applicationChatRecordService.remove(Wrappers.<ApplicationChatRecordEntity>lambdaQuery().in(ApplicationChatRecordEntity::getChatId, chatIds));
        }
        return this.removeById(appId);
    }

    @Transactional
    public ApplicationEntity createApp(ApplicationEntity application) {
        if ("WORK_FLOW".equals(application.getType())) {
            application = createWorkflow(application);
        } else {
            application = createSimple(application);
        }
        ApplicationAccessTokenEntity accessToken = ApplicationAccessTokenEntity.createDefault();
        accessToken.setApplicationId(application.getId());
        accessToken.setLanguage((String) StpUtil.getExtra("language"));
        accessTokenService.save(accessToken);
        return application;
    }

    public ApplicationEntity createWorkflow(ApplicationEntity application) {
        if (Objects.isNull(application.getWorkFlow())) {
            String language = (String) StpUtil.getExtra("language");
            System.out.println("language:" + language);
            Path path = getWorkflowFilePath(language);
            String defaultWorkflowJson = FileUtil.readToString(path.toFile());
            JSONObject workFlow = JSONObject.parseObject(defaultWorkflowJson);
            assert workFlow != null;
            JSONArray nodes = workFlow.getJSONArray("nodes");
            for (int i = 0; i < nodes.size(); i++) {
                JSONObject node = nodes.getJSONObject(i);
                if ("base-node".equals(node.getString("id"))) {
                    JSONObject properties = node.getJSONObject("properties");
                    JSONObject nodeData = properties.getJSONObject("nodeData");
                    nodeData.put("name", application.getName());
                    nodeData.put("desc", application.getDesc());
                    nodeData.put("prologue", application.getPrologue());
                }
            }
            application.setWorkFlow(workFlow);
        }
        application.setUserId(StpUtil.getLoginIdAsString());
        application.setIcon("");
        application.setTtsModelParamsSetting(new JSONObject());
        application.setCleanTime(365);
        application.setFileUploadEnable(false);
        application.setFileUploadSetting(new JSONObject());
        this.save(application);
        return application;
    }


    private Path getWorkflowFilePath(String language) {
        // 获取当前类的绝对路径并转换为文件对象
        File currentClassFile = new File(Objects.requireNonNull(this.getClass().getResource("")).getFile());
        // 获取当前类所在的上级目录
        File parentDir = currentClassFile.getParentFile();
        // 获取当前类所在的上级目录
        File coreParentDir = parentDir.getParentFile().getParentFile();
        String fileName = String.format("default_workflow_%s.json", toLocale(language));
        File coreDir = new File(coreParentDir, "core");
        File workflow = new File(coreDir, "workflow");
        File json = new File(workflow, "json");
        // 构造目标文件路径
        File targetFile = new File(json, fileName);
        // 确认文件存在
        if (!targetFile.exists()) {
            targetFile = new File(coreDir, "/workflow/json/default_workflow_zh.json");
        }
        return targetFile.toPath();
    }

    private static String toLocale(String language) {
        // 实现细节取决于实际的应用逻辑
        if (StringUtils.isNotBlank(language)) {
            if (!language.endsWith("Hant")) {
                language = language.split("-")[0];
            }
            language = language.replaceAll("-", "_");
        }
        return language; // 这里简化处理，直接返回语言代码
    }

    public ApplicationEntity createSimple(ApplicationEntity application) {
        application.setUserId(StpUtil.getLoginIdAsString());
        application.setIcon("");
        application.setWorkFlow(new JSONObject());
        application.setTtsModelParamsSetting(new JSONObject());
        application.setCleanTime(365);
        application.setFileUploadEnable(false);
        application.setFileUploadSetting(new JSONObject());
        this.save(application);
        return application;
    }

    public boolean improveChatLogs(String appId, ChatImproveDTO dto) {
        List<ApplicationChatRecordEntity> chatRecords = applicationChatRecordService.lambdaQuery().in(ApplicationChatRecordEntity::getId, dto.getChatIds()).list();
        List<ParagraphEntity> paragraphs = chatRecords.stream().map(e -> {
            ParagraphEntity paragraphEntity = new ParagraphEntity();
            paragraphEntity.setDatasetId(dto.getDatasetId());
            paragraphEntity.setDocumentId(dto.getDocumentId());
            paragraphEntity.setTitle(e.getProblemText());
            paragraphEntity.setContent(e.getAnswerText());
            paragraphEntity.setHitNum(0);
            paragraphEntity.setIsActive(true);
            paragraphEntity.setStatus("nn0");
            return paragraphEntity;
        }).toList();
        //todo 嵌入到问题数据库里和文本关联
        return paragraphService.saveBatch(paragraphs);
    }

    public ApplicationVO getAppById(String appId) {
        ApplicationEntity entity = this.getById(appId);
        if (entity == null) {
            return null;
        }
        ApplicationVO vo = BeanUtil.copy(entity, ApplicationVO.class);
        List<String> datasetIds = datasetMappingService.getDatasetIdsByAppId(appId);
        vo.setDatasetIdList(datasetIds);
        List<String> mcpIds =mcpMappingService.getMcpIdsByAppId(appId);
        vo.setMcpIdList(mcpIds);
        List<String> functionIds =functionMappingService.getFunctionIdsByAppId(appId);
        vo.setFunctionIdList(functionIds);
        vo.setModel(entity.getModelId());
        vo.setSttModel(entity.getSttModelId());
        vo.setTtsModel(entity.getTtsModelId());
        return vo;
    }


    public String authentication(JSONObject params) {
        String accessToken = params.getString("access_token");
        ApplicationAccessTokenEntity appAccessToken = accessTokenService.lambdaQuery().eq(ApplicationAccessTokenEntity::getAccessToken, accessToken).one();
        if (appAccessToken == null || !appAccessToken.getIsActive()) {
            throw new AccessException("无效的访问令牌");
        } else {
            SaLoginModel loginModel = new SaLoginModel();
            if (StpUtil.isLogin()) {
                UserEntity userEntity = userService.getById(StpUtil.getLoginIdAsString());
                loginModel.setExtra("username", userEntity.getUsername());
                loginModel.setExtra("email", userEntity.getEmail());
                loginModel.setExtra("language", userEntity.getLanguage());
                loginModel.setExtra("client_id", userEntity.getId());
                loginModel.setExtra("client_type", AuthType.ACCESS_TOKEN.name());
                loginModel.setExtra("application_id", appAccessToken.getApplicationId());
                loginModel.setExtra(AuthType.ACCESS_TOKEN.name(), accessToken);
                loginModel.setDevice(AuthType.ACCESS_TOKEN.name());
                StpUtil.login(StpUtil.getLoginId(), loginModel);
            } else {
                loginModel.setExtra("application_id", appAccessToken.getApplicationId());
                loginModel.setExtra("client_id", IdWorker.get32UUID());
                loginModel.setExtra("client_type", AuthType.ACCESS_TOKEN.name());
                loginModel.setDevice(AuthType.ACCESS_TOKEN.name());
                loginModel.setExtra(AuthType.ACCESS_TOKEN.name(), accessToken);
                StpUtil.login(IdWorker.get32UUID(), loginModel);
            }
            return StpUtil.getTokenValue();
        }
    }


    public String authentication1(HttpServletRequest request, JSONObject params) throws Exception {
        String token = request.getHeader("Authorization");
        Map<String, Object> tokenDetails = null;

        try {
            // 校验token
            if (token != null && !token.isEmpty()) {
                tokenDetails = JwtUtil.parseToken(token); // 假设parseToken方法用于解析token
            }
        } catch (Exception e) {
            log.error("token校验失败", e);
        }

    /*    if (withValid) {
            this.isValid(); // 假设有此方法来校验有效性
        }*/

        String accessToken = params.getString("access_token"); // 假设getData()返回一个Map
        ApplicationAccessTokenEntity applicationAccessToken = accessTokenService.lambdaQuery().eq(ApplicationAccessTokenEntity::getAccessToken, accessToken).one();
        if (applicationAccessToken != null && applicationAccessToken.getIsActive()) {
            ApplicationEntity application = this.lambdaQuery().select(ApplicationEntity::getUserId).eq(ApplicationEntity::getId, applicationAccessToken.getApplicationId()).one();
            Map<String, Object> authentication = new HashMap<>();
            JSONObject authenticationValue = params.getJSONObject("authentication_value");
            String clientId;
            if (tokenDetails != null && tokenDetails.containsKey("client_id")) {
                clientId = (String) tokenDetails.get("client_id");
                authentication = (Map<String, Object>) tokenDetails.get("authentication");
            } else {
                clientId = IdWorker.get32UUID();
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

    public JSONObject appProfile() {
        String appId = (String) StpUtil.getExtra("application_id");
        ApplicationEntity application = this.getById(appId);
        ApplicationAccessTokenEntity appAccessToken = accessTokenService.getById(appId);
        JSONObject result = new JSONObject();
        result.put("id", appId);
        result.put("type", application.getType());
        result.put("name", application.getName());
        result.put("desc", application.getDesc());
        result.put("icon", application.getIcon());
        result.put("prologue", application.getPrologue());
        result.put("dialogueNumber", application.getDialogueNumber());
        result.put("multipleRoundsDialogue", application.getDialogueNumber() > 0);
        result.put("fileUploadEnable", application.getFileUploadEnable());
        result.put("fileUploadSetting", application.getFileUploadSetting());
        result.put("sttModelEnable", application.getSttModelEnable());
        result.put("sttAutoSend", application.getSttAutoSend());
        result.put("sttModelId", application.getSttModelId());
        result.put("ttsModelEnable", application.getSttModelEnable());
        result.put("ttsAutoplay", application.getTtsAutoplay());
        result.put("ttsType", application.getTtsType());
        result.put("ttsModelId", application.getTtsModelId());
        result.put("workFlow", application.getWorkFlow());
        result.put("showSource", appAccessToken.getShowSource());
        result.put("language", appAccessToken.getLanguage());
        return result;
    }

    public List<ParagraphVO> hitTest(String id, HitTestDTO dto) {
        List<String> datasetIds = datasetMappingService.getDatasetIdsByAppId(id);
        return retrieveService.paragraphSearch(datasetIds, dto);
    }

    public byte[] playDemoText(String appId, JSONObject data) {
        String ttsModelId = data.getString("ttsModelId");
        BaseTextToSpeech ttsModel = modelService.getModelById(ttsModelId, data);
        return ttsModel.textToSpeech("你好，这里是语音播放测试");
    }

    public byte[] textToSpeech(String id, JSONObject data) {
        String text = data.getString("text");
        ApplicationEntity app = this.getById(id);
        if ("BROWSER".equals(app.getTtsType())) {
            return new byte[0];
        }
        BaseTextToSpeech ttsModel = modelService.getModelById(app.getTtsModelId());
        return ttsModel.textToSpeech(text);
    }

    public boolean editIcon(String id, MultipartFile file) {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(id);
        application.setIcon(imageService.upload(file));
        return this.updateById(application);
    }

    @Transactional
    public Boolean updateAppById(String appId, ApplicationVO appVO) {
        ApplicationEntity application = BeanUtil.copy(appVO, ApplicationEntity.class);
        application.setId(appId);
        JSONObject workFlow = appVO.getWorkFlow();
        if (Objects.nonNull(workFlow) && !workFlow.isEmpty()) {
            JSONArray nodes = workFlow.getJSONArray("nodes");
            JSONObject baseNode = nodes.getJSONObject(0);
            if (Objects.nonNull(baseNode)) {
                String type = baseNode.getString("type");
                if (type.equals("base-node")) {
                    JSONObject properties = baseNode.getJSONObject("properties");
                    JSONObject nodeData = properties.getJSONObject("nodeData");
                    application.setName(nodeData.getString("name"));
                    application.setDesc(nodeData.getString("desc"));
                    application.setPrologue(nodeData.getString("prologue"));
                    application.setFileUploadEnable(nodeData.getBoolean("file_upload_enable"));
                    application.setFileUploadSetting(nodeData.getJSONObject("file_upload_setting"));
                    application.setTtsModelEnable(nodeData.getBoolean("stt_model_enable"));
                    application.setSttModelId(nodeData.getString("stt_model_id"));
                    application.setTtsModelEnable(nodeData.getBoolean("tts_model_enable"));
                    application.setTtsType(nodeData.getString("tts_type"));
                    application.setTtsModelId(nodeData.getString("tts_model_id"));
                    application.setTtsType(nodeData.getString("tts_type"));
                }
            }
        }
        datasetMappingService.updateByAppId(appId,appVO.getDatasetIdList());
        mcpMappingService.updateByAppId(appId,appVO.getMcpIdList());
        functionMappingService.updateByAppId(appId,appVO.getFunctionIdList());
        return this.updateById(application);
    }

    public Boolean publish(String id, JSONObject workflow) {
        if (Objects.nonNull(workflow) && workflow.containsKey("workFlow")) {
            ApplicationEntity application = this.getById(id);
            long count = workFlowVersionService.count(Wrappers.<ApplicationWorkFlowVersionEntity>lambdaQuery().eq(ApplicationWorkFlowVersionEntity::getApplicationId, id));
            ApplicationWorkFlowVersionEntity entity = new ApplicationWorkFlowVersionEntity();
            entity.setWorkFlow(workflow.getJSONObject("workFlow"));
            entity.setApplicationId(id);
            entity.setName(application.getName() + "-V" + (count + 1));
            entity.setPublishUserId(StpUtil.getLoginIdAsString());
            entity.setPublishUserName((String) StpUtil.getExtra("username"));
            return workFlowVersionService.save(entity);
        }
        return false;

    }

    public List<ApplicationEntity> listByUserId(String appId) {
        ApplicationEntity application = this.getById(appId);
        if (Objects.isNull(application)) {
            return Collections.emptyList();
        }
        String userId = StpUtil.getLoginIdAsString();
        String appUserId = application.getUserId();
        if (!userId.equals(appUserId)) {
            return Collections.emptyList();
        }
        return this.lambdaQuery().eq(ApplicationEntity::getUserId, appUserId).list();
    }

    public List<ApplicationWorkFlowVersionEntity> workFlowVersionList(String id) {
        return workFlowVersionService.lambdaQuery().eq(ApplicationWorkFlowVersionEntity::getApplicationId, id).orderByDesc(ApplicationWorkFlowVersionEntity::getCreateTime).list();
    }

    public Boolean updateWorkFlowVersion(String versionId, ApplicationWorkFlowVersionEntity versionEntity) {
        versionEntity.setId(versionId);
        return workFlowVersionService.updateById(versionEntity);
    }

    public void appExport(String id, HttpServletResponse response) throws IOException {
        ApplicationEntity app = this.getById(id);
        MaxKb4J maxKb4J = new MaxKb4J(app, new ArrayList<>(), "v1");
        byte[] bytes = JSONUtil.toJsonStr(maxKb4J).getBytes(StandardCharsets.UTF_8);
        response.setContentType("text/plain");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode(app.getName(), StandardCharsets.UTF_8);
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".mk");
        OutputStream outputStream = response.getOutputStream();
        outputStream.write(bytes);
    }

    @Transactional
    public boolean appImport(MultipartFile file) throws IOException {
        String text = IoUtil.readToString(file.getInputStream());
        MaxKb4J maxKb4J = JSONObject.parseObject(text, MaxKb4J.class);
        ApplicationEntity application = maxKb4J.getApplication();
        application.setId(null);
        boolean flag = this.save(application);
        ApplicationAccessTokenEntity accessToken = ApplicationAccessTokenEntity.createDefault();
        accessToken.setApplicationId(application.getId());
        accessToken.setLanguage((String) StpUtil.getExtra("language"));
        accessTokenService.save(accessToken);
        return flag;
    }

    public List<McpToolVO> convert(String serverName,List<ToolSpecification> tools) {
        return tools.stream().map(tool -> {
            McpToolVO vo = new McpToolVO();
            vo.setServer(serverName);
            vo.setName(tool.name());
            vo.setDescription(tool.description());
            JSONObject json = new JSONObject();
            JSONObject properties = new JSONObject();
            tool.parameters().properties().forEach((k, v) -> {
                JSONObject property = new JSONObject();
                if (v instanceof JsonStringSchema schema) {
                    property.put("type", "string");
                    property.put("description", schema.description());
                } else if (v instanceof JsonNumberSchema schema) {
                    property.put("type", "number");
                    property.put("description", schema.description());
                } else if (v instanceof JsonArraySchema schema) {
                    property.put("type", "array");
                    property.put("description", schema.description());
                } else if (v instanceof JsonBooleanSchema schema) {
                    property.put("type", "boolean");
                    property.put("description", schema.description());
                } else if (v instanceof JsonObjectSchema schema) {
                    property.put("type", "object");
                    property.put("description", schema.description());
                } else if (v instanceof JsonEnumSchema schema) {
                    property.put("type", "enum");
                    property.put("description", schema.description());
                }else if (v instanceof JsonIntegerSchema schema) {
                    property.put("type", "int");
                    property.put("description", schema.description());
                }else if (v instanceof JsonAnyOfSchema schema) {
                    property.put("type", "any");
                    property.put("description", schema.description());
                }else if (v instanceof JsonReferenceSchema schema) {
                    property.put("type", "reference");
                    property.put("description", schema.reference());
                } else {
                    JsonNullSchema schema = (JsonNullSchema) v;
                    property.put("type", "null");
                    property.put("description", "");
                }
                properties.put(k, property);
            });
            json.put("type", "object");
            json.put("properties", properties);
            json.put("required", tool.parameters().required());
            vo.setArgs_schema(json);
            return vo;
        }).collect(Collectors.toList());
    }

    public List<McpToolVO> listTools(String sseUrl) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl(sseUrl)
                .logRequests(true) // if you want to see the traffic in the log
                .logResponses(true)
                .build();
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
        List<ToolSpecification> tools=mcpClient.listTools();
        System.out.println(tools);
        return new ArrayList<>(convert("", tools));
    }

/*    public List<McpToolVO> listTools(JSONObject mcpServer) {
        List<McpToolVO> tools = new ArrayList<>();
        for (String key : mcpServer.keySet()) {
            McpTransport transport = new HttpMcpTransport.Builder()
                    .sseUrl(mcpServer.getJSONObject(key).getString("url"))
                    .logRequests(true) // if you want to see the traffic in the log
                    .logResponses(true)
                    .build();
            McpClient mcpClient = new DefaultMcpClient.Builder()
                    .transport(transport)
                    .build();
            tools.addAll(convert(key, mcpClient.listTools()));
        }
        return tools;
    }*/


    public String speechToText(String appId, MultipartFile file) throws IOException {
        ApplicationEntity app = this.getById(appId);
        BaseSpeechToText sttModel = modelService.getModelById(app.getSttModelId());
        String suffix= Objects.requireNonNull(file.getContentType()).split("/")[1];
        return sttModel.speechToText(file.getBytes(),suffix);
    }

    public void embed(EmbedDTO dto, HttpServletResponse response) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("templates/embed.js");
        ApplicationAccessTokenEntity token = accessTokenService.getByToken(dto.getToken());
        if (token == null || !token.getIsActive()) {
            throw new ApiException("token无效或未被启用");
        }
        Set<String> whiteList = token.getWhiteList();
        if (token.getWhiteActive() && !whiteList.contains(WebUtil.getIP())) {
            throw new ApiException("非法访问，请联系管理员添加白名单");
        }


        String content = IoUtil.readToString(inputStream, StandardCharsets.UTF_8);

        String template = render(content, getParamsMap(token, dto));

        response.setContentType("text/javascript;charset=UTF-8");
        response.getWriter().write(template);
    }

    private Map<String, String> getParamsMap(ApplicationAccessTokenEntity token, EmbedDTO dto) {
        //todo
        String floatIcon = dto.getProtocol() + "://" + dto.getHost() + "/ui/favicon.ico";
        Set<String> whiteList = token.getWhiteList();
        Map<String, String> map = new HashMap<>();
        map.put("is_auth", "true");
        map.put("protocol", dto.getProtocol());
        map.put("query", Optional.ofNullable(dto.getQuery()).orElse(""));
        map.put("host", dto.getHost());
        map.put("token", dto.getToken());
        map.put("white_list_str", whiteList == null ? "" : whiteList.stream().collect(Collectors.joining(System.lineSeparator())));
        map.put("white_active", token.getWhiteActive().toString());
     /*   map.put("float_icon", Optional.ofNullable(token.getFloatIconUrl()).orElse(floatIcon));
        map.put("is_draggable", token.getDraggable().toString());
        map.put("show_history", token.getShowHistory().toString());
        map.put("show_guide", token.getShowGuide().toString());
        ApplicationAccessToken.FloatLocation floatLocation = token.getFloatLocation();
        map.put("x_type", floatLocation.getX().getType());
        map.put("y_type", floatLocation.getY().getType());
        map.put("x_value", floatLocation.getX().getValue().toString());
        map.put("y_value", floatLocation.getY().getValue().toString());*/
        return map;
    }

    private String render(String content, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return content;
    }

    public List<FunctionLibEntity> functionLib(String appId) {
        return functionLibService.list();
    }
    public FunctionLibEntity functionLib(String appId,  String functionId) {
        return functionLibService.getById(functionId);
    }


    public List<FunctionLibEntity> getFunction(String appId) {
        ApplicationEntity app = getById(appId);
        if (app == null) {
            return Collections.emptyList();
        }
        return functionLibService.getUserId(app.getUserId());
    }
}
