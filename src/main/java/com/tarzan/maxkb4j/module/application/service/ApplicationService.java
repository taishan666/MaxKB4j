package com.tarzan.maxkb4j.module.application.service;

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
import com.tarzan.maxkb4j.common.exception.ApiException;
import com.tarzan.maxkb4j.common.util.*;
import com.tarzan.maxkb4j.module.application.domian.dto.ApplicationAccessTokenDTO;
import com.tarzan.maxkb4j.module.application.domian.dto.ApplicationQuery;
import com.tarzan.maxkb4j.module.application.domian.dto.EmbedDTO;
import com.tarzan.maxkb4j.module.application.domian.dto.MaxKb4J;
import com.tarzan.maxkb4j.module.application.domian.entity.*;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.enums.AppType;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.knowledge.consts.SearchType;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeService;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseSpeechToText;
import com.tarzan.maxkb4j.module.model.provider.impl.BaseTextToSpeech;
import com.tarzan.maxkb4j.module.system.permission.constant.AuthTargetType;
import com.tarzan.maxkb4j.module.system.permission.service.UserResourcePermissionService;
import com.tarzan.maxkb4j.module.system.user.domain.entity.UserEntity;
import com.tarzan.maxkb4j.module.system.user.service.UserService;
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

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.BASE;
import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.SEARCH_KNOWLEDGE;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Service
@AllArgsConstructor
public class ApplicationService extends ServiceImpl<ApplicationMapper, ApplicationEntity> {

    private final ModelFactory modelFactory;
    private final KnowledgeService knowledgeService;
    private final UserService userService;
    private final ApplicationAccessTokenService accessTokenService;
    private final ApplicationApiKeyService applicationApiKeyService;
    private final ApplicationChatUserStatsService accessClientService;
    private final ApplicationVersionService applicationVersionService;
    private final ApplicationKnowledgeMappingService knowledgeMappingService;
    private final ApplicationChatRecordService applicationChatRecordService;
    private final ApplicationChatMapper applicationChatMapper;
    private final UserResourcePermissionService userResourcePermissionService;

    public IPage<ApplicationVO> selectAppPage(int page, int size, ApplicationQuery query) {
        Page<ApplicationEntity> appPage = new Page<>(page, size);
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(query.getName())) {
            wrapper.like(ApplicationEntity::getName, query.getName());
        }
        if (StringUtils.isNotBlank(query.getPublishStatus())) {
            wrapper.eq(ApplicationEntity::getIsPublish, "published".equals(query.getPublishStatus()));
        }
        if (Objects.nonNull(query.getCreateUser())) {
            wrapper.eq(ApplicationEntity::getUserId, query.getCreateUser());
        }
        String loginId = StpUtil.getLoginIdAsString();
        UserEntity user = userService.getById(loginId);
        if (Objects.nonNull(user)) {
            if (!CollectionUtils.isEmpty(user.getRole())) {
                if (user.getRole().contains("USER")) {
                    List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.APPLICATION, loginId,query.getFolderId());
                    if (!CollectionUtils.isEmpty(targetIds)) {
                        wrapper.in(ApplicationEntity::getId, targetIds);
                    } else {
                        wrapper.last(" limit 0");
                    }
                }else {
                    if (StringUtils.isNotBlank(query.getFolderId())) {
                        wrapper.eq(ApplicationEntity::getFolderId, query.getFolderId());
                    } else {
                        wrapper.eq(ApplicationEntity::getFolderId, "default");
                    }
                }
            } else {
                wrapper.last(" limit 0");
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


    @Transactional
    public boolean deleteByAppId(String appId) {
        accessTokenService.remove(Wrappers.<ApplicationAccessTokenEntity>lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId));
        applicationApiKeyService.remove(Wrappers.<ApplicationApiKeyEntity>lambdaQuery().eq(ApplicationApiKeyEntity::getApplicationId, appId));
        accessClientService.remove(Wrappers.<ApplicationChatUserStatsEntity>lambdaQuery().eq(ApplicationChatUserStatsEntity::getApplicationId, appId));
        applicationVersionService.remove(Wrappers.<ApplicationVersionEntity>lambdaQuery().eq(ApplicationVersionEntity::getApplicationId, appId));
        knowledgeMappingService.remove(Wrappers.<ApplicationKnowledgeMappingEntity>lambdaQuery().eq(ApplicationKnowledgeMappingEntity::getApplicationId, appId));
        List<String> chatIds = applicationChatMapper.selectList(Wrappers.<ApplicationChatEntity>lambdaQuery().eq(ApplicationChatEntity::getApplicationId, appId)).stream().map(ApplicationChatEntity::getId).toList();
        if (!CollectionUtils.isEmpty(chatIds)) {
            applicationChatMapper.delete(Wrappers.<ApplicationChatEntity>lambdaQuery().eq(ApplicationChatEntity::getApplicationId, appId));
            applicationChatRecordService.remove(Wrappers.<ApplicationChatRecordEntity>lambdaQuery().in(ApplicationChatRecordEntity::getChatId, chatIds));
        }
        userResourcePermissionService.remove(AuthTargetType.APPLICATION, appId);
        return this.removeById(appId);
    }

    @Transactional
    public ApplicationEntity createApp(ApplicationEntity application) {
        application.setKnowledgeSetting(new KnowledgeSetting());
        application.setIcon("./favicon.ico");
        if (AppType.WORK_FLOW.name().equals(application.getType())) {
            application = createWorkflow(application);
        } else {
            application = createSimple(application);
        }
        ApplicationAccessTokenEntity accessToken = ApplicationAccessTokenEntity.createDefault();
        accessToken.setApplicationId(application.getId());
        accessToken.setLanguage((String) StpUtil.getExtra("language"));
        accessTokenService.save(accessToken);
        userResourcePermissionService.ownerSave(AuthTargetType.APPLICATION, application.getId(), application.getUserId());
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
                if (BASE.getKey().equals(node.getString("id"))) {
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
        application.setKnowledgeSetting(getDefaultKnowledgeSetting());
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
        application.setKnowledgeSetting(getDefaultKnowledgeSetting());
        this.save(application);
        return application;
    }

    private KnowledgeSetting getDefaultKnowledgeSetting() {
        KnowledgeSetting knowledgeSetting = new KnowledgeSetting();
        knowledgeSetting.setTopN(5);
        knowledgeSetting.setMaxParagraphCharNumber(5120);
        knowledgeSetting.setSearchMode(SearchType.EMBEDDING);
        knowledgeSetting.setSimilarity(0.6F);
        knowledgeSetting.setNoReferencesSetting(new NoReferencesSetting("ai_questioning", "{question}"));
        return knowledgeSetting;
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

    public ApplicationVO getPublishedDetail(String id) {
        ApplicationVO vo = applicationVersionService.getAppLatestOne(id);
        if (vo == null) {
            return null;
        }
        return wrapVo(vo);
    }


    public ApplicationVO wrapVo(ApplicationVO vo) {
        List<String> knowledgeIds = knowledgeMappingService.getKnowledgeIdsByAppId(vo.getId());
        vo.setKnowledgeIdList(knowledgeIds);
        if (!CollectionUtils.isEmpty(vo.getKnowledgeIdList())) {
            vo.setKnowledgeList(knowledgeService.listByIds(knowledgeIds));
        } else {
            vo.setKnowledgeList(new ArrayList<>());
        }
        if (AppType.WORK_FLOW.name().equals(vo.getType())) {
            JSONObject workFlow = vo.getWorkFlow();
            JSONArray nodes = workFlow.getJSONArray("nodes");
            if (nodes != null) {
                for (int i = 0; i < nodes.size(); i++) {
                    JSONObject node = nodes.getJSONObject(i);
                    if (BASE.getKey().equals(node.getString("type"))) {
                        JSONObject baseNodeProperties = node.getJSONObject("properties"); // 假设每个节点都有 id 字段
                        if (baseNodeProperties != null) {
                            JSONObject nodeData = baseNodeProperties.getJSONObject("nodeData");
                            boolean fileUploadEnable = nodeData.getBooleanValue("fileUploadEnable");
                            vo.setFileUploadEnable(fileUploadEnable);
                            JSONObject fileUploadSetting = nodeData.getJSONObject("fileUploadSetting");
                            vo.setFileUploadSetting(fileUploadSetting);
                            boolean ttsModelEnable = nodeData.getBooleanValue("ttsModelEnable");
                            vo.setTtsModelEnable(ttsModelEnable);
                            boolean ttsAutoplay = nodeData.getBooleanValue("ttsAutoplay");
                            vo.setTtsAutoplay(ttsAutoplay);
                            boolean sttModelEnable = nodeData.getBooleanValue("sttModelEnable");
                            vo.setSttModelEnable(sttModelEnable);
                            boolean sttAutoSend = nodeData.getBooleanValue("sttAutoSend");
                            vo.setSttAutoSend(sttAutoSend);
                            vo.setFileUploadSetting(fileUploadSetting);
                        }
                    }
                    if (SEARCH_KNOWLEDGE.getKey().equals(node.getString("type"))) {
                        JSONObject properties = node.getJSONObject("properties"); // 假设每个节点都有 id 字段
                        if (properties != null) {
                            JSONObject nodeData = properties.getJSONObject("nodeData");
                            JSONArray knowledgeIdListJson = nodeData.getJSONArray("knowledgeIdList");
                            List<String> knowledgeIdList = knowledgeIdListJson.toJavaList(String.class);
                            List<KnowledgeEntity> knowledgeList = vo.getKnowledgeList().stream().filter(k -> knowledgeIdList.contains(k.getId())).toList();
                            nodeData.put("knowledgeList", knowledgeList);
                        }
                    }
                }
            }
        }
        return vo;
    }



    public byte[] playDemoText(String appId, JSONObject modelParams) {
        String ttsModelId = modelParams.getString("ttsModelId");
        BaseTextToSpeech ttsModel = modelFactory.build(ttsModelId, modelParams);
        return ttsModel.textToSpeech("你好，这里是语音播放测试");
    }

    public byte[] textToSpeech(String appId, JSONObject data) {
        String text = data.getString("text");
        ApplicationEntity app = this.getById(appId);
        if ("BROWSER".equals(app.getTtsType())) {
            return new byte[0];
        }
        if (app.getTtsModelId() == null) {
            return new byte[0];
        }
        BaseTextToSpeech ttsModel = modelFactory.build(app.getTtsModelId(), app.getTtsModelParamsSetting());
        return ttsModel.textToSpeech(text);
    }

    @Transactional
    public Boolean updateAppById(String appId, ApplicationVO appVO) {
        ApplicationEntity application = BeanUtil.copy(appVO, ApplicationEntity.class);
        application.setId(appId);
        List<String> knowledgeIds = getKnowledgeIdList(application);
        if (!CollectionUtils.isEmpty(appVO.getKnowledgeIdList())) {
            knowledgeIds.addAll(appVO.getKnowledgeIdList());
        }
        knowledgeMappingService.updateByAppId(appId, knowledgeIds);
        return this.updateById(application);
    }

    private List<String> getKnowledgeIdList(ApplicationEntity entity) {
        List<String> knowledgeIds = new ArrayList<>();
        if (entity == null || entity.getWorkFlow() == null) {
            return knowledgeIds;
        }
        JSONObject workFlow = entity.getWorkFlow();
        JSONArray nodes = workFlow.getJSONArray("nodes");
        if (nodes == null) {
            return knowledgeIds;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (SEARCH_KNOWLEDGE.getKey().equals(node.getString("type"))) {
                JSONObject properties = node.getJSONObject("properties"); // 假设每个节点都有 id 字段
                if (properties == null) {
                    return knowledgeIds;
                }
                JSONObject nodeData = properties.getJSONObject("nodeData");
                if (nodeData == null) {
                    return knowledgeIds;
                }
                JSONArray knowledgeIdList = nodeData.getJSONArray("knowledgeIdList");
                knowledgeIds.addAll(knowledgeIdList.toJavaList(String.class));
            }
        }
        return knowledgeIds;
    }

    @Transactional
    public Boolean publish(String id, JSONObject params) {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(id);
        application.setIsPublish(true);
        application.setPublishTime(new Date());
        this.updateById(application);
        application = this.getById(id);
        long count = applicationVersionService.count(Wrappers.<ApplicationVersionEntity>lambdaQuery().eq(ApplicationVersionEntity::getApplicationId, id));
        ApplicationVersionEntity entity = BeanUtil.copy(application, ApplicationVersionEntity.class);
        entity.setId(null);
        entity.setApplicationId(id);
        entity.setApplicationName(application.getName());
        entity.setName(application.getName() + "-V" + (count + 1));
        entity.setPublishUserId(StpUtil.getLoginIdAsString());
        entity.setPublishUserName((String) StpUtil.getExtra("username"));
        return applicationVersionService.save(entity);
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


    public String speechToText(String appId, MultipartFile file) throws IOException {
        ApplicationEntity app = this.getById(appId);
        BaseSpeechToText sttModel = modelFactory.build(app.getSttModelId());
        String suffix = Objects.requireNonNull(file.getContentType()).split("/")[1];
        return sttModel.speechToText(file.getBytes(), suffix);
    }

    public String embed(EmbedDTO dto) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("templates/embed.txt");
        ApplicationAccessTokenEntity token = accessTokenService.getByToken(dto.getToken());
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
        map.put("is_auth", "true");
        map.put("protocol", dto.getProtocol());
        map.put("query", "");
        map.put("host", dto.getHost());
        map.put("token", dto.getToken());
        map.put("white_list_str", whiteList == null ? "" : whiteList.stream().collect(Collectors.joining(System.lineSeparator())));
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

    //todo 权限
    public List<ApplicationEntity> listApps(String folderId) {
        return this.lambdaQuery().eq(ApplicationEntity::getFolderId, folderId).eq(ApplicationEntity::getIsPublish, true).list();
    }
}
