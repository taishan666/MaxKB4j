package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxkb4j.application.dto.ApplicationDTO;
import com.maxkb4j.application.dto.ApplicationQuery;
import com.maxkb4j.application.dto.ApplicationSimple;
import com.maxkb4j.application.dto.MaxKb4J;
import com.maxkb4j.application.entity.*;
import com.maxkb4j.application.enums.AppType;
import com.maxkb4j.application.mapper.ApplicationMapper;
import com.maxkb4j.application.service.*;
import com.maxkb4j.application.util.ResourceUtil;
import com.maxkb4j.application.vo.ApplicationListVO;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.application.vo.KnowledgeVO;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.core.support.permission.DataPermissionSupport;
import com.maxkb4j.knowledge.dto.KnowledgeSimple;
import com.maxkb4j.knowledge.service.IKnowledgeService;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.tool.dto.ToolDTO;
import com.maxkb4j.tool.service.IToolService;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.maxkb4j.workflow.enums.NodeType.BASE;
import static com.maxkb4j.workflow.enums.NodeType.SEARCH_KNOWLEDGE;


/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, ApplicationEntity> implements IApplicationInternalService {

    private final UserContext userContext;
    private final IKnowledgeService knowledgeService;
    private final IUserService userService;
    private final ApplicationAccessTokenServiceImpl accessTokenService;
    private final ApplicationApiKeyServiceImpl applicationApiKeyService;
    private final ApplicationChatUserStatsService chatUserStatsService;
    private final ApplicationVersionService applicationVersionService;
    private final IUserResourcePermissionService userResourcePermissionService;
    private final DataPermissionSupport dataPermissionSupport;
    private final ApplicationResourceMappingService applicationResourceMappingService;
    private final ApplicationChatShareLinkService applicationChatShareLinkService;
    private final ApplicationLongTermMemoryServiceImpl applicationLongTermMemoryService;
    private final IToolService toolService;

    private static final Cache<String, ApplicationVO> PUBLISHED_APP_CACHE = Caffeine.newBuilder()
            .initialCapacity(64)
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public IPage<ApplicationListVO> selectAppPage(int page, int size, ApplicationQuery query) {
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        return baseMapper.pageList(new Page<>(page, size), query);
    }


    @Transactional
    public boolean deleteByAppId(String appId) {
        PUBLISHED_APP_CACHE.invalidate(appId);
        accessTokenService.remove(Wrappers.<ApplicationAccessTokenEntity>lambdaQuery().eq(ApplicationAccessTokenEntity::getApplicationId, appId));
        applicationApiKeyService.remove(Wrappers.<ApplicationApiKeyEntity>lambdaQuery().eq(ApplicationApiKeyEntity::getApplicationId, appId));
        chatUserStatsService.remove(Wrappers.<ApplicationChatUserStatsEntity>lambdaQuery().eq(ApplicationChatUserStatsEntity::getApplicationId, appId));
        applicationVersionService.remove(Wrappers.<ApplicationVersionEntity>lambdaQuery().eq(ApplicationVersionEntity::getApplicationId, appId));
        userResourcePermissionService.remove(AuthTargetType.APPLICATION, appId);
        // 批量删除资源映射
        applicationResourceMappingService.deleteResourceMappings(appId);
        applicationChatShareLinkService.remove(Wrappers.<ApplicationChatShareLinkEntity>lambdaQuery().eq(ApplicationChatShareLinkEntity::getApplicationId, appId));
        applicationLongTermMemoryService.remove(Wrappers.<ApplicationLongTermMemoryEntity>lambdaQuery().eq(ApplicationLongTermMemoryEntity::getApplicationId, appId));
        return this.removeById(appId);
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
                applicationResourceMappingService.saveResourceMappings(app);
                return app;
            }
        }
        // 非模板方式创建
        application.setIcon("./favicon.ico");
        application.setUserId(userContext.getUserId());
        application.setTtsModelParamsSetting(new JSONObject());
        application.setFileUploadSetting(new JSONObject());
        application.setCleanTime(365);
        application.setWorkFlow(application.getWorkFlow() == null ? new JSONObject() : application.getWorkFlow());
        application.setToolIds(List.of());
        application.setKnowledgeIds(List.of());
        application.setApplicationIds(List.of());
        this.saveApp(application);
        applicationResourceMappingService.saveResourceMappings(application);
        return application;
    }

    public boolean saveMk(MaxKb4J maxKb4j) {
        if (maxKb4j == null) {
            return false;
        }
        Date now = new Date();
        ApplicationEntity application = maxKb4j.getApplication();
        application.setId(null);
        application.setIsPublish(false);
        application.setCreateTime(now);
        application.setUpdateTime(now);
        application.setUserId(userContext.getUserId());
        List<ToolDTO> toolList = maxKb4j.getToolList();
        if (!CollectionUtils.isEmpty(toolList)) {
            toolList.forEach(e -> {
                e.setUserId(userContext.getUserId());
                e.setIsActive(true);
            });
            toolService.saveOrUpdateBatch(toolList);
            List<String> toolIds = toolList.stream().map(ToolDTO::getId).toList();
            application.setToolIds(toolIds);
        }
        return this.saveApp(application);
    }

    @Transactional
    public boolean saveApp(ApplicationEntity application) {
        this.save(application);
        ApplicationAccessTokenEntity accessToken = ApplicationAccessTokenEntity.createDefault();
        accessToken.setApplicationId(application.getId());
        accessToken.setLanguage(userService.getLanguage(application.getUserId()));
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

    @Override
    public ApplicationVO getAppDetail(String appId, boolean debug) {
        if (debug) {
            return this.getDetail(appId);
        } else {
            return this.getPublishedDetail(appId);
        }
    }

    @Override
    public ApplicationSimple getAppSimpleById(String appId) {
        ApplicationEntity app = this.lambdaQuery()
                .select(ApplicationEntity::getId,ApplicationEntity::getName, ApplicationEntity::getDesc, ApplicationEntity::getIcon)
                .eq(ApplicationEntity::getId, appId).one();
        return BeanUtil.copy(app, ApplicationSimple.class);
    }

    @Override
    public List<ApplicationSimple> listAppSimpleByIds(List<String> applicationIds) {
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery(ApplicationEntity.class)
                .select(ApplicationEntity::getId,ApplicationEntity::getName, ApplicationEntity::getDesc, ApplicationEntity::getIcon)
                .in(ApplicationEntity::getId, applicationIds);
        List<ApplicationEntity> list = this.list(wrapper);
        return BeanUtil.copyList(list, ApplicationSimple.class);
    }

    @Override
    public ApplicationVO getDtoById(String id) {
        return BeanUtil.copy(this.getById(id), ApplicationVO.class);
    }

    @Override
    public List<ApplicationVO> listDtoByIds(List<String> ids) {
        return BeanUtil.copyList(this.listByIds(ids), ApplicationVO.class);
    }

    @Override
    public List<Map<String, Object>> listMapsByIds(List<String> ids) {
        return this.listMaps(new LambdaQueryWrapper<ApplicationEntity>().in(ApplicationEntity::getId, ids));
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
        ApplicationVO cached = PUBLISHED_APP_CACHE.get(id, appId -> {
            ApplicationVO vo = applicationVersionService.getAppLatestOne(appId);
            return vo == null ? null : wrapVo(vo);
        });
        return cached == null ? null : BeanUtil.copy(cached, ApplicationVO.class);
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
                            if (nodeData != null) {
                                JSONArray knowledgeIdListJson = nodeData.getJSONArray("knowledgeIds");
                                nodeData.put("knowledgeList", List.of());
                                if (knowledgeIdListJson != null) {
                                    List<String> nodeKnowledgeIds = knowledgeIdListJson.toJavaList(String.class);
                                    if (!CollectionUtils.isEmpty(nodeKnowledgeIds)) {
                                        nodeData.put("knowledgeList", knowledgeService.listSimpleKnowledgeByIds(nodeKnowledgeIds));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            List<String> knowledgeIds = vo.getKnowledgeIds();
            if (!CollectionUtils.isEmpty(knowledgeIds)) {
                List<KnowledgeSimple> knowledgeList = knowledgeService.listSimpleKnowledgeByIds( knowledgeIds);
                vo.setKnowledgeList(BeanUtil.copyList(knowledgeList, KnowledgeVO.class));
            } else {
                vo.setKnowledgeList(List.of());
            }
        }
        return vo;
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
        applicationResourceMappingService.saveResourceMappings(app);
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
        String userId = userContext.getUserId();
        entity.setPublishUserId(userId);
        entity.setPublishUserName(userService.getUsername(userId));
        applicationVersionService.save(entity);
        PUBLISHED_APP_CACHE.invalidate(id);
        return application;
    }

    public List<ApplicationListVO> listApps(String folderId) {
        if (StringUtils.isBlank(folderId)) {
            return Collections.emptyList();
        }
        ApplicationQuery query = new ApplicationQuery();
        query.setFolderId(folderId);
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        return baseMapper.listApps(query);
    }



    @Transactional
    public boolean deleteBatch(List<String> idList) {
        List<Boolean> result = new ArrayList<>();
        for (String id : idList) {
            result.add(deleteByAppId(id));
        }
        return result.stream().allMatch(Boolean::booleanValue);
    }
}
