package com.maxkb4j.application.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.dto.*;
import com.maxkb4j.application.entity.*;
import com.maxkb4j.application.enums.AppType;
import com.maxkb4j.application.mapper.ApplicationChatMapper;
import com.maxkb4j.application.mapper.ApplicationMapper;
import com.maxkb4j.application.util.ResourceUtil;
import com.maxkb4j.application.vo.ApplicationListVO;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.constant.ResourceType;
import com.maxkb4j.common.constant.RoleType;
import com.maxkb4j.common.domain.dto.KnowledgeDTO;
import com.maxkb4j.common.exception.ApiException;
import com.maxkb4j.common.util.*;
import com.maxkb4j.knowledge.entity.KnowledgeEntity;
import com.maxkb4j.knowledge.service.IKnowledgeService;
import com.maxkb4j.model.service.IModelProviderService;
import com.maxkb4j.model.service.STTModel;
import com.maxkb4j.model.service.TTSModel;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.system.entity.TargetResource;
import com.maxkb4j.system.service.IResourceMappingService;
import com.maxkb4j.tool.entity.ToolEntity;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.service.IUserService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

import static com.maxkb4j.workflow.enums.NodeType.*;


/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Service
@RequiredArgsConstructor
public class ApplicationService extends ServiceImpl<ApplicationMapper, ApplicationEntity> implements IApplicationService {

    private final IModelProviderService modelFactory;
    private final IKnowledgeService knowledgeService;
    private final IUserService userService;
    private final ApplicationAccessTokenService accessTokenService;
    private final ApplicationApiKeyService applicationApiKeyService;
    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationVersionService applicationVersionService;
    private final ApplicationChatRecordService applicationChatRecordService;
    private final ApplicationChatMapper applicationChatMapper;
    private final IUserResourcePermissionService userResourcePermissionService;
    private final IToolService toolService;
    private final IResourceMappingService resourceMappingService;

    public IPage<ApplicationVO> selectAppPage(int page, int size, ApplicationQuery query) {
        Page<ApplicationEntity> appPage = new Page<>(page, size);
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(ApplicationEntity::getName, query.getName());
        }
        if (StringUtils.isNotBlank(query.getPublishStatus())) {
            wrapper.eq(ApplicationEntity::getIsPublish, "published".equals(query.getPublishStatus()));
        }
        if (StringUtils.isNotBlank(query.getType())) {
            wrapper.eq(ApplicationEntity::getType, query.getType());
        }
        if (Objects.nonNull(query.getCreateUser())) {
            wrapper.eq(ApplicationEntity::getUserId, query.getCreateUser());
        }
        String loginId = StpKit.ADMIN.getLoginIdAsString();
        Set<String> role = userService.getRoleById(loginId);
        if (!CollectionUtils.isEmpty(role)) {
            if (role.contains(RoleType.USER)) {
                List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.APPLICATION, loginId);
                if (!CollectionUtils.isEmpty(targetIds)) {
                    wrapper.in(ApplicationEntity::getId, targetIds);
                } else {
                    wrapper.last(" limit 0");
                }
            } else {
                if (StringUtils.isNotBlank(query.getFolderId())) {
                    wrapper.eq(ApplicationEntity::getFolderId, query.getFolderId());
                } else {
                    wrapper.eq(ApplicationEntity::getFolderId, "default");
                }
            }
        } else {
            wrapper.last(" limit 0");
        }
        wrapper.orderByDesc(ApplicationEntity::getCreateTime);
        this.page(appPage, wrapper);
        Map<String, String> nicknameMap = userService.getNicknameMap();
        return PageUtil.copy(appPage, app -> {
            ApplicationVO vo = BeanUtil.copy(app, ApplicationVO.class);
            vo.setNickname(nicknameMap.get(app.getUserId()));
            return vo;
        });
    }


    @Transactional
    public boolean deleteByAppId(String appId) {
        accessTokenService.remove(Wrappers.<ApplicationAccessTokenEntity>lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId));
        applicationApiKeyService.remove(Wrappers.<ApplicationApiKeyEntity>lambdaQuery().eq(ApplicationApiKeyEntity::getApplicationId, appId));
        chatUserStatsService.remove(Wrappers.<ApplicationChatUserStatsEntity>lambdaQuery().eq(ApplicationChatUserStatsEntity::getApplicationId, appId));
        applicationVersionService.remove(Wrappers.<ApplicationVersionEntity>lambdaQuery().eq(ApplicationVersionEntity::getApplicationId, appId));
        List<String> chatIds = applicationChatMapper.selectList(Wrappers.<ApplicationChatEntity>lambdaQuery().eq(ApplicationChatEntity::getApplicationId, appId)).stream().map(ApplicationChatEntity::getId).toList();
        if (!CollectionUtils.isEmpty(chatIds)) {
            applicationChatMapper.delete(Wrappers.<ApplicationChatEntity>lambdaQuery().eq(ApplicationChatEntity::getApplicationId, appId));
            applicationChatRecordService.remove(Wrappers.<ApplicationChatRecordEntity>lambdaQuery().in(ApplicationChatRecordEntity::getChatId, chatIds));
        }
        userResourcePermissionService.remove(AuthTargetType.APPLICATION, appId);
        // 批量删除资源映射
        resourceMappingService.deleteBySourceId(ResourceType.APPLICATION, appId);
        return this.removeById(appId);
    }

    private void saveResourceMappings(ApplicationEntity app) {
        List<String> modelIds = new ArrayList<>(Stream.of(app.getModelId(), app.getSttModelId(), app.getTtsModelId())
                .filter(Objects::nonNull)
                .toList());
        List<String> knowledgeIds = app.getKnowledgeIds() == null ? new ArrayList<>() : app.getKnowledgeIds();
        List<String> toolIds = app.getToolIds() == null ? new ArrayList<>() : app.getToolIds();
        JSONObject workFlow = app.getWorkFlow();
        if (workFlow != null && workFlow.containsKey("nodes")) {
            JSONArray nodes = workFlow.getJSONArray("nodes");
            if (nodes != null) {
                for (int i = 0; i < nodes.size(); i++) {
                    JSONObject node = nodes.getJSONObject(i);
                    JSONObject properties = node.getJSONObject("properties");
                    if (properties != null && properties.containsKey("nodeData")) {
                        JSONObject nodeData = properties.getJSONObject("nodeData");
                        if (nodeData != null && nodeData.containsKey("toolLibId")) {
                            toolIds.add(nodeData.getString("toolLibId"));
                        }
                        if (nodeData != null && nodeData.containsKey("mcpToolId")) {
                            toolIds.add(nodeData.getString("mcpToolId"));
                        }
                        if (nodeData != null && nodeData.containsKey("toolIds")) {
                            toolIds.addAll((Collection<? extends String>) nodeData.get("toolIds"));
                        }
                        if (nodeData != null && nodeData.containsKey("knowledgeIds")) {
                            knowledgeIds.addAll((Collection<? extends String>) nodeData.get("knowledgeIds"));
                        }
                        if (nodeData != null && nodeData.containsKey("modelId")) {
                            modelIds.add(nodeData.getString("modelId"));
                        }
                        if (nodeData != null && nodeData.containsKey("ttsModelId")) {
                            modelIds.add(nodeData.getString("ttsModelId"));
                        }
                        if (nodeData != null && nodeData.containsKey("sttModelId")) {
                            modelIds.add(nodeData.getString("sttModelId"));
                        }
                        if (nodeData != null && nodeData.containsKey("rerankerModelId")) {
                            modelIds.add(nodeData.getString("rerankerModelId"));
                        }
                    }
                }
            }
        }
        saveResourceMappings(app.getId(), knowledgeIds, toolIds, modelIds);
    }

    /**
     * 批量保存资源映射关系
     */
    private void saveResourceMappings(String appId,
                                      List<String> knowledgeIds,
                                      List<String> toolIds,
                                      List<String> modelIds) {
        knowledgeIds = knowledgeIds == null ? List.of() : knowledgeIds.stream().filter(Objects::nonNull).toList();
        List<TargetResource> targets = new ArrayList<>(knowledgeIds.stream().map(id -> new TargetResource(id, ResourceType.KNOWLEDGE)).toList());
        toolIds = toolIds == null ? List.of() : toolIds.stream().filter(Objects::nonNull).toList();
        targets.addAll(toolIds.stream().map(id -> new TargetResource(id, ResourceType.TOOL)).toList());
        targets.addAll(modelIds.stream().filter(Objects::nonNull).map(id -> new TargetResource(id, ResourceType.MODEL)).toList());
        resourceMappingService.relation(ResourceType.APPLICATION, appId, targets);
    }


    @Transactional
    public ApplicationEntity createApp(ApplicationDTO application) {
        JSONObject workFlowTemplate = application.getWorkFlowTemplate();
        if (workFlowTemplate != null) {
            String downloadUrl = workFlowTemplate.getString("downloadUrl");
            if (StringUtils.isNotBlank(downloadUrl)) {
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource resource = resolver.getResource("templates/app/" + downloadUrl);
                MaxKb4J maxKb4j = ResourceUtil.parseMk(resource);
                ApplicationEntity app = maxKb4j.getApplication();
                app.setName(application.getName());
                app.setDesc(application.getDesc());
                app.setIcon(StringUtils.isNotBlank(application.getIcon()) ? application.getIcon() : app.getIcon());
                saveMk(maxKb4j);
                saveResourceMappings(app);
                return app;
            }
        }
        // 非模板方式创建
        application.setIcon("./favicon.ico");
        application.setUserId(StpKit.ADMIN.getLoginIdAsString());
        application.setTtsModelParamsSetting(new JSONObject());
        application.setFileUploadSetting(new JSONObject());
        application.setCleanTime(365);
        application.setWorkFlow(application.getWorkFlow() == null ? new JSONObject() : application.getWorkFlow());
        application.setToolIds(List.of());
        application.setKnowledgeIds(List.of());
        application.setApplicationIds(List.of());
        this.saveApp(application);
        saveResourceMappings(application);
        return application;
    }

    @Transactional
    public boolean appImport(InputStream inputStream) {
        MaxKb4J maxKb4j = ResourceUtil.parseMk(inputStream);
        return saveMk(maxKb4j);
    }

    @Transactional
    protected boolean saveApp(ApplicationEntity application) {
        this.save(application);
        ApplicationAccessTokenEntity accessToken = ApplicationAccessTokenEntity.createDefault();
        accessToken.setApplicationId(application.getId());
        accessToken.setLanguage((String) StpKit.ADMIN.getExtra("language"));
        accessTokenService.save(accessToken);
        return userResourcePermissionService.ownerSave(AuthTargetType.APPLICATION, application.getId(), application.getUserId());
    }

    public ApplicationVO appProfile(String appId) {
        ApplicationVO appProfile = this.getDetail(appId);
        if (appProfile == null || !appProfile.getIsPublish()) {
            return appProfile;
        }
        return this.getPublishedDetail(appId);
    }

    public ApplicationVO getAppDetail(String appId, boolean debug) {
        if (debug) {
            return this.getDetail(appId);
        } else {
            return this.getPublishedDetail(appId);
        }
    }

    public ApplicationVO getDetail(String id) {
        ApplicationEntity entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        ApplicationVO vo = BeanUtil.copy(entity, ApplicationVO.class);
        return wrapVo(vo);
    }

    private ApplicationVO getPublishedDetail(String id) {
        ApplicationVO vo = applicationVersionService.getAppLatestOne(id);
        if (vo == null) {
            return null;
        }
        return wrapVo(vo);
    }


    public ApplicationVO wrapVo(ApplicationVO vo) {
        if (AppType.WORK_FLOW.name().equals(vo.getType())) {
            JSONObject workFlow = vo.getWorkFlow();
            JSONArray nodes = workFlow.getJSONArray("nodes");
            if (nodes != null) {
                for (int i = 0; i < nodes.size(); i++) {
                    JSONObject node = nodes.getJSONObject(i);
                    if (SEARCH_KNOWLEDGE.getKey().equals(node.getString("type"))) {
                        JSONObject properties = node.getJSONObject("properties"); // 假设每个节点都有 id 字段
                        if (properties != null) {
                            JSONObject nodeData = properties.getJSONObject("nodeData");
                            JSONArray knowledgeIdListJson = nodeData.getJSONArray("knowledgeIds");
                            nodeData.put("knowledgeList", List.of());
                            if (knowledgeIdListJson != null) {
                                List<String> nodeKnowledgeIds = knowledgeIdListJson.toJavaList(String.class);
                                if (!CollectionUtils.isEmpty(nodeKnowledgeIds)) {
                                    nodeData.put("knowledgeList", knowledgeService.listByIds(nodeKnowledgeIds));
                                }
                            }
                        }
                    }
                }
            }
        } else {
            List<String> knowledgeIds = vo.getKnowledgeIds();
            if (!CollectionUtils.isEmpty(knowledgeIds)) {
                List<KnowledgeEntity> knowledgeList = knowledgeService.lambdaQuery().select(KnowledgeEntity::getId, KnowledgeEntity::getName).in(KnowledgeEntity::getId, knowledgeIds).orderByDesc(KnowledgeEntity::getCreateTime).list();
                vo.setKnowledgeList(BeanUtil.copyList(knowledgeList, KnowledgeDTO.class));
            } else {
                vo.setKnowledgeList(List.of());
            }
        }
        return vo;
    }


    public byte[] playDemoText(String appId, JSONObject modelParams) {
        String ttsModelId = modelParams.getString("ttsModelId");
        TTSModel ttsModel = modelFactory.buildTTSModel(ttsModelId, modelParams);
        return ttsModel.textToSpeech("你好，这里是语音播放测试");
    }

    public byte[] textToSpeech(String appId, JSONObject data, boolean debug) {
        String text = data.getString("text");
        ApplicationEntity app = this.getAppDetail(appId, debug);
        if ("BROWSER".equals(app.getTtsType())) {
            return new byte[0];
        }
        if (app.getTtsModelId() == null) {
            return new byte[0];
        }
        TTSModel ttsModel = modelFactory.buildTTSModel(app.getTtsModelId(), app.getTtsModelParamsSetting());
        return ttsModel.textToSpeech(text);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public Boolean updateAppById(String appId, ApplicationVO appVO) {
        ApplicationEntity app = BeanUtil.copy(appVO, ApplicationEntity.class);
        app.setId(appId);
        JSONObject workFlow = appVO.getWorkFlow();
        if (workFlow != null && workFlow.containsKey("nodes")) {
            JSONArray nodes = workFlow.getJSONArray("nodes");
            if (nodes != null) {
                nodes.stream()
                        .filter(node -> node instanceof Map)
                        .map(node -> (Map<String, Object>) node)
                        .filter(node -> BASE.getKey().equals(node.get("type")))
                        .findFirst()
                        .map(JSONObject::new).ifPresent(baseNode -> updateAppFromBaseNode(app, baseNode));
            }
        }
        saveResourceMappings(app);
        return this.updateById(app);
    }

    /**
     * 从基础节点更新应用配置
     */
    private void updateAppFromBaseNode(ApplicationEntity app, JSONObject baseNode) {
        JSONObject baseNodeProperties = baseNode.getJSONObject("properties");
        if (baseNodeProperties == null) {
            return;
        }
        JSONObject nodeData = baseNodeProperties.getJSONObject("nodeData");
        if (nodeData == null) {
            return;
        }
        // 更新应用基础信息
        app.setName(nodeData.getString("name"));
        app.setDesc(nodeData.getString("desc"));
        app.setPrologue(nodeData.getString("prologue"));
        app.setFileUploadEnable(nodeData.getBooleanValue("fileUploadEnable"));
        app.setFileUploadSetting(nodeData.getJSONObject("fileUploadSetting"));
        app.setTtsType(nodeData.getString("ttsType"));
        app.setTtsModelEnable(nodeData.getBooleanValue("ttsModelEnable"));
        app.setTtsModelId(nodeData.getString("ttsModelId"));
        app.setTtsModelParamsSetting(nodeData.getJSONObject("ttsModelParamsSetting"));
        app.setTtsAutoplay(nodeData.getBooleanValue("ttsAutoplay"));
        app.setSttModelEnable(nodeData.getBooleanValue("sttModelEnable"));
        app.setSttModelId(nodeData.getString("sttModelId"));
        app.setSttAutoSend(nodeData.getBooleanValue("sttAutoSend"));
    }


    @Transactional
    public ApplicationEntity publish(String id, JSONObject params) {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(id);
        application.setIsPublish(true);
        application.setPublishTime(new Date());
        this.updateById(application);
        application = this.getById(id);
        ApplicationVersionEntity entity = BeanUtil.copy(application, ApplicationVersionEntity.class);
        entity.setId(null);
        entity.setApplicationId(id);
        entity.setApplicationName(application.getName());
        entity.setName(DateTimeUtil.now());
        entity.setPublishUserId(StpKit.ADMIN.getLoginIdAsString());
        entity.setPublishUserName((String) StpKit.ADMIN.getExtra("username"));
        applicationVersionService.save(entity);
        return application;
    }

    public String speechToText(String appId, MultipartFile file, boolean debug) throws IOException {
        ApplicationEntity app = this.getAppDetail(appId, debug);
        STTModel sttModel = modelFactory.buildSTTModel(app.getSttModelId());
        String suffix = Objects.requireNonNull(file.getContentType()).split("/")[1];
        return sttModel.speechToText(file.getBytes(), suffix);
    }

    public String embed(EmbedDTO dto) {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("templates/embed.txt");
        ApplicationAccessTokenEntity token = accessTokenService.getByAccessToken(dto.getToken());
        if (token == null || !token.getIsActive()) {
            throw new ApiException("token无效或未被启用");
        }
        List<String> whiteList = token.getWhiteList();
        if (token.getWhiteActive() && !whiteList.contains(WebUtil.getIP())) {
            throw new ApiException("非法访问，请联系管理员添加白名单");
        }
        String content = IoUtil.readToString(inputStream, StandardCharsets.UTF_8);
        return render(content, getParamsMap(token, dto));
    }

    private Map<String, String> getParamsMap(ApplicationAccessTokenEntity token, EmbedDTO dto) {
        String floatIcon = dto.getProtocol() + "://" + dto.getHost() + "/chat/MaxKB.gif";
        List<String> whiteList = token.getWhiteList();
        Map<String, String> map = new HashMap<>();
        map.put("is_auth", String.valueOf(token.getIsActive()));
        map.put("protocol", dto.getProtocol());
        map.put("query", "");
        map.put("host", dto.getHost());
        map.put("token", dto.getToken());
        map.put("white_list_str", whiteList == null ? "" : String.join(",", whiteList));
        map.put("white_active", token.getWhiteActive().toString());
        map.put("float_icon", floatIcon);
        map.put("is_draggable", "false");
        map.put("show_guide", "false");
        map.put("x_type", "right");
        map.put("y_type", "bottom");
        map.put("x_value", "0");
        map.put("y_value", "30");
        map.put("max_kb_id", IdWorker.get32UUID());
        map.put("header_font_color", "rgb(100, 106, 115");
        return map;
    }

    private String render(String content, Map<String, String> variables) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return content;
    }

    public List<ApplicationListVO> listApps(String folderId) {
        String userId = StpKit.ADMIN.getLoginIdAsString();
        Set<String> role = userService.getRoleById(userId);
        List<ApplicationEntity> list;
        if (role.contains(RoleType.ADMIN)) {
            list = this.lambdaQuery().eq(ApplicationEntity::getIsPublish, true).orderByDesc(ApplicationEntity::getCreateTime).list();
        } else {
            List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.APPLICATION, userId);
            if (targetIds.isEmpty()) {
                return Collections.emptyList();
            }
            list = this.lambdaQuery().in(ApplicationEntity::getId, targetIds).eq(ApplicationEntity::getIsPublish, true).orderByDesc(ApplicationEntity::getCreateTime).list();
        }
        return list.stream().filter(e -> folderId.equals(e.getFolderId())).map(e -> BeanUtil.copy(e, ApplicationListVO.class)).toList();
    }

    public Flux<Map<String, String>> promptGenerate(String appId, String modelId, PromptGenerateDTO dto) {
        ApplicationEntity app = this.getById(appId);
        StreamingChatModel chatModel = modelFactory.buildStreamingChatModel(modelId, null);
        List<ChatMessage> messages = dto.getMessages().stream()
                .map(message -> {
                    if ("user".equals(message.getRole())) {
                        return UserMessage.from(message.getContent());
                    } else if ("ai".equals(message.getRole())) {
                        return AiMessage.from(message.getContent());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
        if (messages.isEmpty()) {
            return Flux.error(new IllegalArgumentException("No user message found to generate prompt"));
        }
        String prompt = dto.getPrompt();
        String detail = StringUtils.isBlank(app.getDesc()) ? app.getName() : app.getDesc();
        prompt = prompt.replace("{application_name}", app.getName())
                .replace("{detail}", detail)
                .replace("{userInput}", dto.getMessages().get(messages.size() - 1).getContent());
        List<ChatMessage> finalMessages = new ArrayList<>(messages);
        finalMessages.set(finalMessages.size() - 1, UserMessage.from(prompt));
        Sinks.Many<Map<String, String>> sink = Sinks.many().unicast().onBackpressureBuffer();
        chatModel.chat(finalMessages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.tryEmitNext(Map.of("content", partialResponse));
            }

            @Override
            public void onCompleteResponse(ChatResponse chatResponse) {
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable throwable) {
                sink.tryEmitError(throwable);
            }
        });
        return sink.asFlux();
    }


    @Transactional
    boolean saveMk(MaxKb4J maxKb4j) {
        if (maxKb4j == null) {
            return false;
        }
        Date now = new Date();
        ApplicationEntity application = maxKb4j.getApplication();
        application.setId(null);
        application.setIsPublish(false);
        application.setCreateTime(now);
        application.setUpdateTime(now);
        application.setUserId(StpKit.ADMIN.getLoginIdAsString());
        List<ToolEntity> toolList = maxKb4j.getToolList();
        if (!CollectionUtils.isEmpty(toolList)) {
            toolList.forEach(e -> {
                e.setUserId(StpKit.ADMIN.getLoginIdAsString());
                e.setIsActive(true);
                e.setCreateTime(now);
                e.setUpdateTime(now);
            });
            toolService.saveOrUpdateBatch(toolList);
            List<String> toolIds = toolList.stream().map(ToolEntity::getId).toList();
            application.setToolIds(toolIds);
        }
        return this.saveApp(application);
    }


    public Boolean delMulApplication(List<String> idList) {
        Boolean result = false;
        for (String id : idList) {
            result = deleteByAppId(id);
        }
        return result;
    }
}
