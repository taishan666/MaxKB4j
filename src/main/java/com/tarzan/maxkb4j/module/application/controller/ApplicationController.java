package com.tarzan.maxkb4j.module.application.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.core.common.dto.QueryDTO;
import com.tarzan.maxkb4j.module.application.dto.ApplicationAccessTokenDTO;
import com.tarzan.maxkb4j.module.application.dto.ChatImproveDTO;
import com.tarzan.maxkb4j.module.application.dto.EmbedDTO;
import com.tarzan.maxkb4j.module.application.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.entity.ApplicationWorkFlowVersionEntity;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.application.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.vo.McpToolVO;
import com.tarzan.maxkb4j.module.dataset.dto.HitTestDTO;
import com.tarzan.maxkb4j.module.dataset.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.functionlib.entity.FunctionLibEntity;
import com.tarzan.maxkb4j.module.mcplib.entity.McpLibEntity;
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

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application")
    public R<List<ApplicationEntity>> listApps() {
        return R.success(applicationService.list());
    }

    @SaCheckPermission("APPLICATION:CREATE")
    @PostMapping("api/application")
    public R<ApplicationEntity> createApp(@RequestBody ApplicationEntity application) {
        return R.success(applicationService.createApp(application));
    }

    @SaCheckPermission("APPLICATION:CREATE")
    @PostMapping("api/application/import")
    public R<Boolean> appImport(MultipartFile file) throws Exception {
        return R.status(applicationService.appImport(file));
    }

    @SaCheckPermission("APPLICATION:CREATE")
    @PostMapping("api/application/authentication")
    public R<String> authentication(@RequestBody JSONObject params) throws Exception {
        return R.success(applicationService.authentication(params));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{id}/function_lib")
    public R<List<FunctionLibEntity>> functionLib(@PathVariable("id") String id) {
        return R.success(applicationService.functionLib(id));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{id}/function_lib/{functionId}")
    public R<FunctionLibEntity> functionLib(@PathVariable("id") String id,@PathVariable("functionId") String functionId) {
        return R.success(applicationService.functionLib(id,functionId));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{id}/work_flow_version")
    public R<List<ApplicationWorkFlowVersionEntity>> workFlowVersionList(@PathVariable("id") String id) {
        return R.success(applicationService.workFlowVersionList(id));
    }

    @SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("api/application/{id}/work_flow_version/{versionId}")
    public R<Boolean> updateWorkFlowVersion(@PathVariable("id") String id,@PathVariable("versionId") String versionId,@RequestBody ApplicationWorkFlowVersionEntity versionEntity) {
        return R.success(applicationService.updateWorkFlowVersion(versionId,versionEntity));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{id}/hit_test")
    public R<List<ParagraphVO>> hitTest(@PathVariable("id") String id, HitTestDTO dto) {
        return R.success(applicationService.hitTest(id, dto));
    }

    @SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("api/application/{id}/edit_icon")
    public R<Boolean> editIcon(@PathVariable("id") String id, MultipartFile file) {
        return R.success(applicationService.editIcon(id, file));
    }

    @SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("api/application/{id}/publish")
    public R<Boolean> publish(@PathVariable("id") String id, @RequestBody JSONObject workflow) {
        return R.success(applicationService.publish(id, workflow));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/profile")
    public R<JSONObject> appProfile() {
        return R.success(applicationService.appProfile());
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{id}/export")
    public void appExport(@PathVariable("id") String id,HttpServletResponse response) throws IOException {
        applicationService.appExport(id,response);
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{page}/{size}")
    public R<IPage<ApplicationEntity>> userApplications(@PathVariable("page") int page, @PathVariable("size") int size, QueryDTO query) {
        return R.success(applicationService.selectAppPage(page, size, query));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{id}")
    public R<ApplicationVO> getByAppId(@PathVariable("id") String id) {
        return R.success(applicationService.getAppById(id));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{id}/application/{appId}")
    public R<ApplicationVO> application(@PathVariable("id") String id,@PathVariable("appId") String appId) {
        return R.success(applicationService.getAppById(appId));
    }


    @SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("api/application/{id}")
    public R<Boolean> updateByAppId(@PathVariable("id") String id, @RequestBody ApplicationVO appVO) {
        return R.success(applicationService.updateAppById(id, appVO));
    }

    @SaCheckPermission("APPLICATION:DELETE")
    @DeleteMapping("api/application/{id}")
    public R<Boolean> deleteByAppId(@PathVariable("id") String id) {
        return R.success(applicationService.deleteByAppId(id));
    }

    @SaCheckPermission("APPLICATION:READ")
    @PostMapping("api/application/{appId}/play_demo_text")
    public ResponseEntity<byte[]> playDemoText(@PathVariable("appId") String appId, @RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mp3"));
        headers.setContentDispositionFormData("attachment", "abc.mp3");
        return new ResponseEntity<>(applicationService.playDemoText(appId, data), headers, HttpStatus.OK);
    }

    @SaCheckPermission("APPLICATION:READ")
    @PostMapping("api/application/{appId}/text_to_speech")
    public ResponseEntity<byte[]> textToSpeech(@PathVariable("appId") String appId, @RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mp3"));
        headers.setContentDispositionFormData("attachment", "abc.mp3");
        return new ResponseEntity<>(applicationService.textToSpeech(appId, data), headers, HttpStatus.OK);
    }

    @SaCheckPermission("APPLICATION:READ")
    @PostMapping("api/application/{appId}/speech_to_text")
    public  R<String> speechToText(@PathVariable("appId") String appId, MultipartFile file) throws IOException {
        return  R.data(applicationService.speechToText(appId,file));
    }


    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{appId}/application")
    public R<List<ApplicationEntity>> listByUserId(@PathVariable("appId") String appId) {
        return R.success(applicationService.listByUserId(appId));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{appId}/access_token")
    public R<ApplicationAccessTokenEntity> getAccessToken(@PathVariable("appId") String appId) {
        return R.success(applicationService.getAccessToken(appId));
    }

    @SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("api/application/{appId}/access_token")
    public R<ApplicationAccessTokenEntity> updateAccessToken(@PathVariable("appId") String appId, @RequestBody ApplicationAccessTokenDTO dto) {
        return R.success(applicationService.updateAccessToken(appId, dto));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{appId}/model")
    public R<List<ModelEntity>> model(@PathVariable("appId") String appId, String modelType) {
        return R.success(applicationService.getAppModels(appId, modelType));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{appId}/list_dataset")
    public R<List<DatasetEntity>> listDataset(@PathVariable("appId") String appId) {
        return R.success(applicationService.getDataset(appId));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{appId}/list_mcp")
    public R<List<McpLibEntity>> listMcp(@PathVariable("appId") String appId) {
        return R.success(applicationService.getMcp(appId));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{appId}/list_function")
    public R<List<FunctionLibEntity>> listFunction(@PathVariable("appId") String appId) {
        return R.success(applicationService.getFunction(appId));
    }

    @SaCheckPermission("APPLICATION:CREATE")
    @PostMapping("api/application/{appId}/dataset/{datasetId}/improve")
    public R<Boolean> improveChatLogs(@PathVariable("appId") String appId, @PathVariable("appId") String datasetId, ChatImproveDTO dto) {
        return R.success(applicationService.improveChatLogs(appId, dto));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/{appId}/model_params_form/{modelId}")
    public R<JSONArray> modelParams(@PathVariable("appId") String appId, @PathVariable("modelId") String modelId) {
        return R.success(applicationService.modelParams(appId, modelId));
    }

    @SaCheckPermission("APPLICATION:READ")
    @GetMapping("api/application/mcp_servers")
    public R<List<McpToolVO>> mcpServers(String sseUrl) {
        return R.success(applicationService.listTools(sseUrl));
    }



    /**
     * 嵌入第三方
     *
     * @param dto      dto
     * @param response response
     */
    @GetMapping("/embed")
    @SaIgnore
    public void embed(EmbedDTO dto, HttpServletResponse response) throws IOException {
        applicationService.embed(dto,response);
    }




}
