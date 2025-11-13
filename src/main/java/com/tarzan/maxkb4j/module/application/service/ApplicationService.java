package com.tarzan.maxkb4j.module.application.service;

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
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationListVO;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.enums.AppType;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationChatMapper;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.knowledge.consts.SearchType;
import com.tarzan.maxkb4j.module.knowledge.domain.entity.KnowledgeEntity;
import com.tarzan.maxkb4j.module.knowledge.service.KnowledgeService;
import com.tarzan.maxkb4j.module.model.custom.base.STTModel;
import com.tarzan.maxkb4j.module.model.custom.base.TTSModel;
import com.tarzan.maxkb4j.module.model.info.service.ModelFactory;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
    private final ApplicationChatUserStatsService chatUserStatsService;
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
        if (StringUtils.isNotBlank(query.getType())) {
            wrapper.eq(ApplicationEntity::getType, query.getType());
        }
        if (Objects.nonNull(query.getCreateUser())) {
            wrapper.eq(ApplicationEntity::getUserId, query.getCreateUser());
        }
        String loginId = StpKit.ADMIN.getLoginIdAsString();
        UserEntity user = userService.getById(loginId);
        if (Objects.nonNull(user)) {
            if (!CollectionUtils.isEmpty(user.getRole())) {
                if (user.getRole().contains("USER")) {
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
        chatUserStatsService.remove(Wrappers.<ApplicationChatUserStatsEntity>lambdaQuery().eq(ApplicationChatUserStatsEntity::getApplicationId, appId));
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
        application.setDialogueNumber(0);
        application.setUserId(StpKit.ADMIN.getLoginIdAsString());
        application.setTtsModelParamsSetting(new JSONObject());
        application.setCleanTime(365);
        application.setFileUploadEnable(false);
        application.setFileUploadSetting(new JSONObject());
        application.setKnowledgeSetting(getDefaultKnowledgeSetting());
        if (application.getWorkFlow()== null){
            application.setWorkFlow(new JSONObject());
        }
        this.save(application);
        ApplicationAccessTokenEntity accessToken = ApplicationAccessTokenEntity.createDefault();
        accessToken.setApplicationId(application.getId());
        accessToken.setLanguage((String) StpKit.ADMIN.getExtra("language"));
        accessTokenService.save(accessToken);
        userResourcePermissionService.ownerSave(AuthTargetType.APPLICATION, application.getId(), application.getUserId());
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
        TTSModel ttsModel = modelFactory.buildTTSModel(ttsModelId, modelParams);
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
        TTSModel ttsModel = modelFactory.buildTTSModel(app.getTtsModelId(), app.getTtsModelParamsSetting());
        return ttsModel.textToSpeech(text);
    }

    @Transactional
    public Boolean updateAppById(String appId, ApplicationVO appVO) {
        ApplicationEntity app = BeanUtil.copy(appVO, ApplicationEntity.class);
        app.setId(appId);
        List<String> knowledgeIds = getKnowledgeIdList(app);
        if (!CollectionUtils.isEmpty(appVO.getKnowledgeIdList())) {
            knowledgeIds.addAll(appVO.getKnowledgeIdList());
        }
        knowledgeMappingService.updateByAppId(appId, knowledgeIds);
        JSONObject workFlow = appVO.getWorkFlow();
        if (workFlow!=null && workFlow.containsKey("nodes")) {
            JSONArray nodes = workFlow.getJSONArray("nodes");
            if (nodes != null) {
                @SuppressWarnings("unchecked")
                JSONObject baseNode = nodes.stream()
                        .filter(node -> node instanceof Map)
                        .map(node -> (Map<String, Object>) node)
                        .filter(node -> BASE.getKey().equals(node.get("type")))
                        .findFirst()
                        .map(JSONObject::new) // 将 Map 转为 JSONObject
                        .orElse(null);
                if (baseNode != null) {
                    JSONObject baseNodeProperties = baseNode.getJSONObject("properties");
                    if (baseNodeProperties != null) {
                        JSONObject nodeData = baseNodeProperties.getJSONObject("nodeData");
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
                }
            }
        }
        return this.updateById(app);
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
        entity.setPublishUserId(StpKit.ADMIN.getLoginIdAsString());
        entity.setPublishUserName((String) StpKit.ADMIN.getExtra("username"));
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
        accessToken.setLanguage((String) StpKit.ADMIN.getExtra("language"));
        accessTokenService.save(accessToken);
        return flag;
    }


    public String speechToText(String appId, MultipartFile file) throws IOException {
        ApplicationEntity app = this.getById(appId);
        STTModel sttModel = modelFactory.buildSTTModel(app.getSttModelId());
        String suffix = Objects.requireNonNull(file.getContentType()).split("/")[1];
        return sttModel.speechToText(file.getBytes(), suffix);
    }

    public String embed(EmbedDTO dto) throws IOException {
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
        List<String> targetIds = userResourcePermissionService.getTargetIds(AuthTargetType.APPLICATION, userId);
        if (targetIds.isEmpty()){
            return new ArrayList<>();
        }
        List<ApplicationEntity> list= this.lambdaQuery().in(ApplicationEntity::getId, targetIds).eq(ApplicationEntity::getIsPublish, true).list();
        return list.stream().filter(e -> folderId.equals(e.getFolderId())).map(e -> BeanUtil.copy(e, ApplicationListVO.class)).toList();
    }
}
