package com.maxkb4j.application.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.maxkb4j.application.dto.*;
import com.maxkb4j.application.entity.ApplicationAccessTokenEntity;
import com.maxkb4j.application.entity.ApplicationEntity;
import com.maxkb4j.application.entity.ApplicationVersionEntity;
import com.maxkb4j.application.mapper.ApplicationMapper;
import com.maxkb4j.application.service.ApplicationCascadeDeleteService;
import com.maxkb4j.application.service.ApplicationDetailAssembler;
import com.maxkb4j.application.service.ApplicationMkImportService;
import com.maxkb4j.application.service.ApplicationResourceMappingService;
import com.maxkb4j.application.service.ApplicationVersionService;
import com.maxkb4j.application.service.IApplicationAccessTokenInternalService;
import com.maxkb4j.application.service.IApplicationInternalService;
import com.maxkb4j.application.service.PublishedApplicationCache;
import com.maxkb4j.application.util.WorkFlowNodes;
import com.maxkb4j.application.vo.ApplicationListVO;
import com.maxkb4j.application.vo.ApplicationVO;
import com.maxkb4j.common.context.UserContext;
import com.maxkb4j.common.util.BeanUtil;
import com.maxkb4j.common.util.DateTimeUtil;
import com.maxkb4j.core.support.permission.DataPermissionSupport;
import com.maxkb4j.system.constant.AuthTargetType;
import com.maxkb4j.tool.dto.ToolDTO;
import com.maxkb4j.user.service.IUserResourcePermissionService;
import com.maxkb4j.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.maxkb4j.workflow.enums.NodeType.BASE;

/**
 * 应用服务实现：聚焦应用自身的增删改查、发布与详情查询编排。
 * <p>
 * MK 模板导入、详情组装、发布缓存与级联删除等职责已分别抽离至
 * {@link ApplicationMkImportService}、{@link ApplicationDetailAssembler}、
 * {@link PublishedApplicationCache} 与 {@link ApplicationCascadeDeleteService}。
 *
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, ApplicationEntity> implements IApplicationInternalService {

    private final UserContext userContext;
    private final IUserService userService;
    private final IApplicationAccessTokenInternalService accessTokenService;
    private final ApplicationVersionService applicationVersionService;
    private final IUserResourcePermissionService userResourcePermissionService;
    private final DataPermissionSupport dataPermissionSupport;
    private final ApplicationResourceMappingService applicationResourceMappingService;
    private final ApplicationMkImportService mkImportService;
    private final ApplicationDetailAssembler detailAssembler;
    private final PublishedApplicationCache publishedApplicationCache;
    private final ApplicationCascadeDeleteService cascadeDeleteService;

    @Override
    public IPage<ApplicationListVO> selectAppPage(int page, int size, ApplicationQuery query) {
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        return baseMapper.pageList(new Page<>(page, size), query);
    }

    @Override
    @Transactional
    public boolean deleteByAppId(String appId) {
        cascadeDeleteService.deleteRelatedResources(appId);
        publishedApplicationCache.invalidate(appId);
        return this.removeById(appId);
    }

    @Override
    @Transactional
    public boolean deleteBatch(List<String> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return true;
        }
        dataPermissionSupport.checkManagePermission(AuthTargetType.APPLICATION, idList);
        boolean success = true;
        for (String id : idList) {
            success &= deleteByAppId(id);
        }
        return success;
    }

    @Override
    public boolean batchCleanTime(ApplicationBatchEditDTO dto) {
        List<String> idList=dto.getIdList();
        if (CollectionUtils.isEmpty(idList)) {
            return true;
        }
        dataPermissionSupport.checkManagePermission(AuthTargetType.APPLICATION, idList);
        return this.lambdaUpdate().set(ApplicationEntity::getCleanTime, dto.getCleanTime()).in(ApplicationEntity::getId, idList).update();
    }

    @Override
    @Transactional
    public ApplicationEntity createApp(ApplicationDTO application) {
        String downloadUrl = getTemplateDownloadUrl(application.getWorkFlowTemplate());
        if (StringUtils.isNotBlank(downloadUrl)) {
            return createAppFromTemplate(downloadUrl, application);
        }
        // 非模板方式创建
        application.setIcon("./favicon.ico");
        application.setUserId(userContext.getUserId());
        application.setTtsModelParamsSetting(new JSONObject());
        application.setFileUploadSetting(new JSONObject());
        application.setCleanTime(365);
        application.setWorkFlow(application.getWorkFlow() == null ? new JSONObject() : application.getWorkFlow());
        application.setToolIds(new ArrayList<>());
        application.setKnowledgeIds(new ArrayList<>());
        application.setApplicationIds(new ArrayList<>());
        this.saveOrUpdateApp(application);
        applicationResourceMappingService.saveResourceMappings(application);
        return application;
    }

    @Transactional
    protected ApplicationEntity createAppFromTemplate(String downloadUrl, ApplicationDTO application) {
        MaxKb4J maxKb4j = mkImportService.loadClasspathTemplate(downloadUrl);
        ApplicationEntity app = maxKb4j.getApplication();
        app.setId(null);
        app.setName(application.getName());
        app.setDesc(application.getDesc());
        if (StringUtils.isNotBlank(application.getIcon())) {
            app.setIcon(application.getIcon());
        }
        mkImportService.normalizeForImport(app, maxKb4j.getToolList());
        this.saveOrUpdateApp(app);
        applicationResourceMappingService.saveResourceMappings(app);
        return app;
    }

    @Override
    @Transactional
    public boolean upsertMk(ApplicationEntity app, List<ToolDTO> toolList) {
        mkImportService.normalizeForImport(app, toolList);
        boolean result = this.saveOrUpdateApp(app);
        publishedApplicationCache.invalidate(app.getId());
        return result;
    }

    @Transactional
    public boolean saveOrUpdateApp(ApplicationEntity application) {
        if (application.getId() == null) {
            this.save(application);
            ApplicationAccessTokenEntity accessToken = ApplicationAccessTokenEntity.createDefault();
            accessToken.setApplicationId(application.getId());
            accessToken.setLanguage(userService.getLanguage(application.getUserId()));
            accessTokenService.save(accessToken);
            return userResourcePermissionService.ownerSave(AuthTargetType.APPLICATION, application.getId(), application.getUserId());
        }
        return this.updateById(application);
    }

    @Override
    public ApplicationVO appProfile(String appId) {
        ApplicationVO draft = this.getDetail(appId);
        if (draft == null || !Boolean.TRUE.equals(draft.getIsPublish())) {
            return draft;
        }
        return publishedApplicationCache.get(appId);
    }

    @Override
    public ApplicationVO getAppDetail(String appId, boolean debug) {
        if (debug) {
            return this.getDetail(appId);
        }
        return publishedApplicationCache.get(appId);
    }

    @Override
    public ApplicationSimple getAppSimpleById(String appId) {
        ApplicationEntity app = this.lambdaQuery()
                .select(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getDesc, ApplicationEntity::getIcon)
                .eq(ApplicationEntity::getId, appId).one();
        return BeanUtil.copy(app, ApplicationSimple.class);
    }

    @Override
    public List<ApplicationSimple> listAppSimpleByIds(List<String> applicationIds) {
        LambdaQueryWrapper<ApplicationEntity> wrapper = Wrappers.lambdaQuery(ApplicationEntity.class)
                .select(ApplicationEntity::getId, ApplicationEntity::getName, ApplicationEntity::getDesc, ApplicationEntity::getIcon)
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

    @Override
    public ApplicationVO getDetail(String id) {
        ApplicationEntity entity = this.getById(id);
        if (entity == null) {
            return null;
        }
        return detailAssembler.wrap(BeanUtil.copy(entity, ApplicationVO.class));
    }

    @Override
    @Transactional
    public Boolean updateAppById(ApplicationDTO appDTO) {
        String downloadUrl = getTemplateDownloadUrl(appDTO.getWorkFlowTemplate());
        if (StringUtils.isNotBlank(downloadUrl)) {
            return updateAppFromTemplate(downloadUrl, appDTO);
        }
        syncFromBaseNode(appDTO);
        applicationResourceMappingService.saveResourceMappings(appDTO);
        publishedApplicationCache.invalidate(appDTO.getId());
        return this.updateById(appDTO);
    }

    @Transactional
    protected Boolean updateAppFromTemplate(String downloadUrl, ApplicationDTO appDTO) {
        MaxKb4J maxKb4j = mkImportService.loadClasspathTemplate(downloadUrl);
        ApplicationEntity app = maxKb4j.getApplication();
        app.setId(appDTO.getId());
        boolean status = this.upsertMk(app, maxKb4j.getToolList());
        applicationResourceMappingService.saveResourceMappings(app);
        return status;
    }

    /**
     * 从工作流基础节点同步应用基础配置。
     */
    private void syncFromBaseNode(ApplicationDTO appDTO) {
        JSONObject baseNode = findBaseNode(appDTO.getWorkFlow());
        if (baseNode == null) {
            return;
        }
        JSONObject nodeData = WorkFlowNodes.getNodeData(baseNode);
        if (nodeData == null) {
            return;
        }
        appDTO.setName(nodeData.getString("name"));
        appDTO.setDesc(nodeData.getString("desc"));
        appDTO.setPrologue(nodeData.getString("prologue"));
        appDTO.setFileUploadEnable(nodeData.getBooleanValue("fileUploadEnable"));
        appDTO.setFileUploadSetting(nodeData.getJSONObject("fileUploadSetting"));
        appDTO.setTtsType(nodeData.getString("ttsType"));
        appDTO.setTtsModelEnable(nodeData.getBooleanValue("ttsModelEnable"));
        appDTO.setTtsModelId(nodeData.getString("ttsModelId"));
        appDTO.setTtsModelParamsSetting(nodeData.getJSONObject("ttsModelParamsSetting"));
        appDTO.setTtsAutoplay(nodeData.getBooleanValue("ttsAutoplay"));
        appDTO.setSttModelEnable(nodeData.getBooleanValue("sttModelEnable"));
        appDTO.setSttModelId(nodeData.getString("sttModelId"));
        appDTO.setSttAutoSend(nodeData.getBooleanValue("sttAutoSend"));
    }

    private static JSONObject findBaseNode(JSONObject workFlow) {
        JSONArray nodes = WorkFlowNodes.getNodes(workFlow);
        if (nodes == null) {
            return null;
        }
        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            if (node != null && BASE.getKey().equals(node.getString("type"))) {
                return node;
            }
        }
        return null;
    }

    private static String getTemplateDownloadUrl(JSONObject workFlowTemplate) {
        return workFlowTemplate == null ? null : workFlowTemplate.getString("downloadUrl");
    }

    @Override
    @Transactional
    public ApplicationEntity publish(String id, JSONObject params) {
        ApplicationEntity application = this.getById(id);
        if (application == null) {
            return null;
        }
        application.setIsPublish(true);
        application.setPublishTime(new Date());
        this.updateById(application);
        applicationVersionService.save(buildVersion(application));
        publishedApplicationCache.invalidate(id);
        return application;
    }

    private ApplicationVersionEntity buildVersion(ApplicationEntity application) {
        ApplicationVersionEntity entity = BeanUtil.copy(application, ApplicationVersionEntity.class);
        entity.setId(null);
        entity.setApplicationId(application.getId());
        entity.setApplicationName(application.getName());
        entity.setName(DateTimeUtil.now());
        String userId = userContext.getUserId();
        entity.setPublishUserId(userId);
        entity.setPublishUserName(userService.getUsername(userId));
        return entity;
    }

    @Override
    public List<ApplicationListVO> listApps(String folderId) {
        if (StringUtils.isBlank(folderId)) {
            return Collections.emptyList();
        }
        ApplicationQuery query = new ApplicationQuery();
        query.setFolderId(folderId);
        dataPermissionSupport.fill(query, AuthTargetType.APPLICATION);
        return baseMapper.listApps(query);
    }
}
