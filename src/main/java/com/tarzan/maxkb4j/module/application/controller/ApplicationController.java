package com.tarzan.maxkb4j.module.application.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.core.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.application.dto.ApplicationAccessTokenDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatImproveDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationWorkFlowVersionEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.application.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.vo.McpToolVO;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.model.info.entity.ModelEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Tag(name = "APP应用管理", description = "APP应用管理")
@RestController
@AllArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("api/application")
    public R<List<ApplicationEntity>> listApps() {
        return R.success(applicationService.list());
    }

    @PostMapping("api/application")
    public R<ApplicationEntity> createApp(@RequestBody ApplicationEntity application) {
        return R.success(applicationService.createApp(application));
    }

    @PostMapping("api/application/import")
    public R<Boolean> appImport(MultipartFile file) throws Exception {
        return R.status(applicationService.appImport(file));
    }

    @PostMapping("api/application/authentication")
    public R<String> authentication(@RequestBody JSONObject params) throws Exception {
        return R.success(applicationService.authentication(params));
    }

    @GetMapping("api/application/{id}/function_lib")
    public R<List<String>> functionLib(@PathVariable("id") String id) {
        return R.success(List.of());
    }

    @GetMapping("api/application/{id}/work_flow_version")
    public R<List<ApplicationWorkFlowVersionEntity>> workFlowVersionList(@PathVariable("id") String id) {
        return R.success(applicationService.workFlowVersionList(id));
    }

    @PutMapping("api/application/{id}/work_flow_version/{versionId}")
    public R<Boolean> updateWorkFlowVersion(@PathVariable("id") String id,@PathVariable("versionId") String versionId,@RequestBody ApplicationWorkFlowVersionEntity versionEntity) {
        return R.success(applicationService.updateWorkFlowVersion(versionId,versionEntity));
    }

    @GetMapping("api/application/{id}/hit_test")
    public R<List<ParagraphVO>> hitTest(@PathVariable("id") String id, HitTestDTO dto) {
        return R.success(applicationService.hitTest(id, dto));
    }

    @PutMapping("api/application/{id}/edit_icon")
    public R<Boolean> editIcon(@PathVariable("id") String id, MultipartFile file) {
        return R.success(applicationService.editIcon(id, file));
    }

    @PutMapping("api/application/{id}/publish")
    public R<Boolean> publish(@PathVariable("id") String id, @RequestBody JSONObject workflow) {
        return R.success(applicationService.publish(id, workflow));
    }

    @GetMapping("api/application/profile")
    public R<JSONObject> appProfile() {
        return R.success(applicationService.appProfile());
    }

    @GetMapping("api/application/{id}/export")
    public void appExport(@PathVariable("id") String id,HttpServletResponse response) throws IOException {
        applicationService.appExport(id,response);
    }

    @GetMapping("api/application/{page}/{size}")
    public R<IPage<ApplicationEntity>> userApplications(@PathVariable("page") int page, @PathVariable("size") int size, QueryDTO query) {
        return R.success(applicationService.selectAppPage(page, size, query));
    }

    @GetMapping("api/application/{appId}")
    public R<ApplicationVO> getByAppId(@PathVariable("appId") String appId) {
        return R.success(applicationService.getAppById(appId));
    }

   // @SaCheckPermission("APPLICATION:MANAGE")
    @DeleteMapping("api/application/{appId}")
    public R<Boolean> deleteByAppId(@PathVariable("appId") String appId) {
        return R.success(applicationService.deleteByAppId(appId));
    }

    @PostMapping("api/application/{appId}/play_demo_text")
    public ResponseEntity<byte[]> playDemoText(@PathVariable("appId") String appId, @RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mp3"));
        headers.setContentDispositionFormData("attachment", "abc.mp3");
        return new ResponseEntity<>(applicationService.playDemoText(appId, data), headers, HttpStatus.OK);
    }

    @PostMapping("api/application/{appId}/text_to_speech")
    public ResponseEntity<byte[]> textToSpeech(@PathVariable("appId") String appId, @RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mp3"));
        headers.setContentDispositionFormData("attachment", "abc.mp3");
        return new ResponseEntity<>(applicationService.textToSpeech(appId, data), headers, HttpStatus.OK);
    }

    @PutMapping("api/application/{appId}")
    public R<Boolean> updateByAppId(@PathVariable("appId") String appId, @RequestBody ApplicationVO appVO) {
        return R.success(applicationService.updateAppById(appId, appVO));
    }

    @GetMapping("api/application/{appId}/application")
    public R<List<ApplicationEntity>> listByUserId(@PathVariable("appId") String appId) {
        return R.success(applicationService.listByUserId(appId));
    }

    @GetMapping("api/application/{appId}/access_token")
    public R<ApplicationAccessTokenEntity> getAccessToken(@PathVariable("appId") String appId) {
        return R.success(applicationService.getAccessToken(appId));
    }

    @PutMapping("api/application/{appId}/access_token")
    public R<ApplicationAccessTokenEntity> updateAccessToken(@PathVariable("appId") String appId, @RequestBody ApplicationAccessTokenDTO dto) {
        return R.success(applicationService.updateAccessToken(appId, dto));
    }

    @GetMapping("api/application/{appId}/model")
    public R<List<ModelEntity>> model(@PathVariable("appId") String appId, String modelType) {
        return R.success(applicationService.getAppModels(appId, modelType));
    }

    @GetMapping("api/application/{appId}/list_dataset")
    public R<List<DatasetEntity>> datasets(@PathVariable("appId") String appId) {
        return R.success(applicationService.getDatasets(appId));
    }

    @PostMapping("api/application/{appId}/dataset/{datasetId}/improve")
    public R<Boolean> improveChatLogs(@PathVariable("appId") String appId, @PathVariable("appId") String datasetId, ChatImproveDTO dto) {
        return R.success(applicationService.improveChatLogs(appId, dto));
    }

    @GetMapping("api/application/{appId}/model_params_form/{modelId}")
    public R<JSONArray> modelParams(@PathVariable("appId") String appId, @PathVariable("modelId") String modelId) {
        return R.success(applicationService.modelParams(appId, modelId));
    }

    @GetMapping("api/application/mcp_servers")
    public R<List<McpToolVO>> mcpServers(String url, String type) {
        return R.success(applicationService.mcpServers(url,type));
    }



}
