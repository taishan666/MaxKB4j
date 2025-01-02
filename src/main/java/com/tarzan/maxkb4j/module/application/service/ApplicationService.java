package com.tarzan.maxkb4j.module.application.service;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tarzan.maxkb4j.module.application.dto.ChatImproveDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.*;
import com.tarzan.maxkb4j.module.application.mapper.ApplicationMapper;
import com.tarzan.maxkb4j.module.application.vo.ApplicationPublicAccessClientStatisticsVO;
import com.tarzan.maxkb4j.module.application.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.service.DatasetService;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.module.model.service.ModelService;
import com.tarzan.maxkb4j.util.BeanUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
        modelType=StringUtils.isBlank(modelType)?"LLM":modelType;
        ApplicationEntity app = getById(appId);
        if (app == null) {
            return Collections.emptyList();
        }
        return modelService.getUserIdAndType(app.getUserId(), modelType);
    }


    public ApplicationAccessTokenEntity getAccessToken(UUID appId) {
        return accessTokenService.accessToken(appId);
    }

    public boolean updateAccessToken(UUID appId,ApplicationAccessTokenEntity entity) {
        entity.setApplicationId(appId);
        return accessTokenService.updateById(entity);
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
        return chatService.chatLogs(chatPage, UUID.fromString(appId),query);
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
        return this.removeById(appId);
    }

    public ApplicationEntity createApp(ApplicationEntity application) {
        String userId= StpUtil.getLoginIdAsString();
        application.setUserId(UUID.fromString(userId));
        application.setIcon("");
        application.setWorkFlow(new JSONObject());
        application.setTtsModelParamsSetting(new JSONObject());
        application.setCleanTime(1000*365);
        application.setFileUploadEnable(false);
        application.setFileUploadSetting(new JSONObject());
        this.save(application);
        return application;
    }

    public boolean improveChatLogs(UUID appId, ChatImproveDTO dto) {
        return false;
    }

    public ApplicationVO getAppById(UUID appId) {
        ApplicationEntity entity=this.getById(appId);
        if (entity == null) {
            return null;
        }
        ApplicationVO vo= BeanUtil.copy(entity,ApplicationVO.class);
        List<UUID> datasetIds=new ArrayList<>();
        List<ApplicationDatasetMappingEntity> mappingEntities=applicationDatasetMappingService.lambdaQuery()
                .select(ApplicationDatasetMappingEntity::getDatasetId)
                .eq(ApplicationDatasetMappingEntity::getApplicationId,appId).list();
        if(!CollectionUtils.isEmpty(mappingEntities)){
            datasetIds=mappingEntities.stream().map(ApplicationDatasetMappingEntity::getDatasetId).toList();
        }
        vo.setDatasetIdList(datasetIds);
        return vo;
    }

    // 定义日期格式
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public List<ApplicationStatisticsVO>  statistics(UUID appId,ChatQueryDTO query) {
        List<ApplicationStatisticsVO> result=new ArrayList<>();
        List<ApplicationStatisticsVO> list= applicationChatService.statistics(appId,query);
        List<ApplicationPublicAccessClientStatisticsVO> AccessClientList=applicationPublicAccessClientService.statistics(appId,query);
        // 将字符串解析为LocalDate对象
        LocalDate startDate = LocalDate.parse(query.getStartTime(), formatter);
        LocalDate endDate = LocalDate.parse(query.getEndTime(), formatter);
        // 遍历从开始日期到结束日期之间的所有日期
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String day = date.format(formatter);
            ApplicationStatisticsVO vo=getApplicationStatisticsVO(list,day);
            ApplicationPublicAccessClientStatisticsVO accessClientStatisticsVO=getApplicationPublicAccessClientStatisticsVO(AccessClientList,day);
            if(accessClientStatisticsVO!=null){
                vo.setCustomerAddedCount(accessClientStatisticsVO.getCustomerAddedCount());
            }
            result.add(vo);
        }
        return result;
    }

    public ApplicationStatisticsVO getApplicationStatisticsVO(List<ApplicationStatisticsVO> list,String day){
        if(!CollectionUtils.isEmpty(list)){
            Optional<ApplicationStatisticsVO> optional= list.stream().filter(e->e.getDay().equals(day)).findFirst();
            if(optional.isPresent()){
                return optional.get();
            }
        }
        ApplicationStatisticsVO vo=new ApplicationStatisticsVO();
        vo.setDay(day);
        vo.setStarNum(0);
        vo.setTokensNum(0);
        vo.setCustomerNum(0);
        vo.setChatRecordCount(0);
        vo.setTrampleNum(0);
        return vo;
    }

    public ApplicationPublicAccessClientStatisticsVO getApplicationPublicAccessClientStatisticsVO(List<ApplicationPublicAccessClientStatisticsVO> list,String day){
        if(!CollectionUtils.isEmpty(list)){
            Optional<ApplicationPublicAccessClientStatisticsVO> optional= list.stream().filter(e->e.getDay().equals(day)).findFirst();
            if(optional.isPresent()){
                return optional.get();
            }
        }
        return null;
    }

    public List<ApplicationApiKeyEntity> listApikey(UUID appId) {
        return applicationApiKeyService.lambdaQuery().eq(ApplicationApiKeyEntity::getApplicationId, appId).list();
    }

    public boolean createApikey(UUID appId) {
        ApplicationApiKeyEntity entity=new ApplicationApiKeyEntity();
        entity.setApplicationId(appId);
        entity.setIsActive(true);
        entity.setAllowCrossDomain(false);
        String uuid=UUID.randomUUID().toString();
        entity.setSecretKey("maxKb4j-"+uuid.replaceAll("-",""));
        entity.setUserId(UUID.fromString(StpUtil.getLoginIdAsString()));
        entity.setCrossDomainList(new HashSet<>());
        return applicationApiKeyService.save(entity);
    }

    public boolean updateApikey(UUID appId,UUID apikeyId,ApplicationApiKeyEntity entity) {
        entity.setId(apikeyId);
        return applicationApiKeyService.updateById(entity);
    }

    public boolean deleteApikey(UUID appId,UUID apikeyId) {
        return applicationApiKeyService.removeById(apikeyId);
    }
}
