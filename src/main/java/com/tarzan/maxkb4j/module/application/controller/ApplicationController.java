package com.tarzan.maxkb4j.module.application.controller;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.module.application.dto.ChatImproveDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatQueryDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationApiKeyEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.application.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationChatEntity;
import com.tarzan.maxkb4j.module.application.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.model.entity.ModelEntity;
import com.tarzan.maxkb4j.tool.api.R;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@RestController
@AllArgsConstructor
public class ApplicationController{

    @Autowired
    private ApplicationService applicationService;

    @GetMapping("api/application")
    public R<List<ApplicationEntity>> listApps(){
        return R.success(applicationService.list());
    }

    @PostMapping("api/application")
    public R<ApplicationEntity> createApp(@RequestBody ApplicationEntity application){
        return R.success(applicationService.createApp(application));
    }

    @GetMapping("api/application/{page}/{size}")
    public R<IPage<ApplicationEntity>> userApplications(@PathVariable("page")int page, @PathVariable("size")int size, QueryDTO query){
        return R.success(applicationService.selectAppPage(page,size,query));
    }

    @GetMapping("api/application/{appId}")
    public R<ApplicationVO> getByAppId(@PathVariable("appId") String appId){
        return R.success(applicationService.getAppById(UUID.fromString(appId)));
    }

    @DeleteMapping("api/application/{appId}")
    public R<Boolean> deleteByAppId(@PathVariable("appId") String appId){
        return R.success(applicationService.deleteByAppId(UUID.fromString(appId)));
    }

    @PutMapping("api/application/{appId}")
    public R<Boolean> updateByAppId(@PathVariable("appId") String appId,@RequestBody ApplicationEntity entity){
        entity.setId(UUID.fromString(appId));
        return R.success(applicationService.updateById(entity));
    }

    @GetMapping("api/application/{appId}/application")
    public R<ApplicationEntity> getByAppId1(@PathVariable("appId") String appId){
        return R.success(applicationService.getById(UUID.fromString(appId)));
    }

    @GetMapping("api/application/{appId}/access_token")
    public R<ApplicationAccessTokenEntity> getAccessToken(@PathVariable("appId") UUID appId){
        return R.success(applicationService.getAccessToken(appId));
    }

    @PutMapping("api/application/{appId}/access_token")
    public R<Boolean> updateAccessToken(@PathVariable("appId") UUID appId,@RequestBody ApplicationAccessTokenEntity entity){
        return R.success(applicationService.updateAccessToken(appId,entity));
    }

    @GetMapping("api/application/{appId}/api_key")
    public R<List<ApplicationApiKeyEntity>> listApikey(@PathVariable("appId") UUID appId){
        return R.success(applicationService.listApikey(appId));
    }

    @PostMapping("api/application/{appId}/api_key")
    public R<Boolean> createApikey(@PathVariable("appId") UUID appId){
        return R.success(applicationService.createApikey(appId));
    }

    @PutMapping("api/application/{appId}/api_key/{apiKeyId}")
    public R<Boolean> updateApikey(@PathVariable("appId") UUID appId, @PathVariable("apiKeyId") UUID apiKeyId, @RequestBody ApplicationApiKeyEntity apiKeyEntity){
        return R.success(applicationService.updateApikey(appId,apiKeyId,apiKeyEntity));
    }

    @DeleteMapping("api/application/{appId}/api_key/{apiKeyId}")
    public R<Boolean> deleteApikey(@PathVariable("appId") UUID appId,@PathVariable("apiKeyId") UUID apiKeyId){
        return R.success(applicationService.deleteApikey(appId,apiKeyId));
    }

    @GetMapping("api/application/{appId}/model")
    public R<List<ModelEntity>> model(@PathVariable("appId") String appId, String model_type){
        return R.success(applicationService.getAppModels(UUID.fromString(appId),model_type));
    }

    @GetMapping("api/application/{appId}/list_dataset")
    public R<List<DatasetEntity>> datasets(@PathVariable("appId") String appId){
        return R.success(applicationService.getDatasets(UUID.fromString(appId)));
    }

    @PostMapping("api/application/{appId}/dataset/{datasetId}/improve")
    public R<Boolean> improveChatLogs(@PathVariable("appId") UUID appId, @PathVariable("appId") UUID datasetId, ChatImproveDTO dto){
        return R.success(applicationService.improveChatLogs(appId,dto));
    }

    @GetMapping("api/application/{appId}/chat/{page}/{size}")
    public R<IPage<ApplicationChatEntity>> chatLogs(@PathVariable("appId") String appId, @PathVariable("page")int page, @PathVariable("size")int size, HttpServletRequest request){
        ChatQueryDTO query = new ChatQueryDTO();
        query.setKeyword(request.getParameter("abstract"));
        query.setStartTime(request.getParameter("start_time"));
        query.setEndTime(request.getParameter("end_time"));
        return R.success(applicationService.chatLogs(appId,page,size,query));
    }

    @GetMapping("api/application/{appId}/model_params_form/{modelId}")
    public R<JSONArray> modelParams(@PathVariable("appId") String appId, @PathVariable("modelId")String modelId){
        return R.success(applicationService.modelParams(appId,modelId));
    }

    @GetMapping("api/application/{appId}/statistics/chat_record_aggregate_trend")
    public R<List<ApplicationStatisticsVO>> statistics(@PathVariable("appId") UUID appId,HttpServletRequest request){
        ChatQueryDTO query = new ChatQueryDTO();
        query.setKeyword(request.getParameter("abstract"));
        query.setStartTime(request.getParameter("start_time"));
        query.setEndTime(request.getParameter("end_time"));
        return R.success(applicationService.statistics(appId,query));
    }

}
