package com.tarzan.maxkb4j.module.application.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaIgnore;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.constant.AppConst;
import com.tarzan.maxkb4j.core.api.R;
import com.tarzan.maxkb4j.core.common.dto.Query;
import com.tarzan.maxkb4j.module.application.domian.dto.ApplicationAccessTokenDTO;
import com.tarzan.maxkb4j.module.application.domian.dto.ChatImproveDTO;
import com.tarzan.maxkb4j.module.application.domian.dto.EmbedDTO;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.McpToolVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.dataset.domain.dto.DataSearchDTO;
import com.tarzan.maxkb4j.module.dataset.domain.entity.DatasetEntity;
import com.tarzan.maxkb4j.module.dataset.domain.vo.ParagraphVO;
import com.tarzan.maxkb4j.module.functionlib.domain.entity.FunctionLibEntity;
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
@RequestMapping(AppConst.ADMIN_PATH+"/workspace/default")
@AllArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application")
    public R<List<ApplicationEntity>> listApps() {
        return R.success(applicationService.list());
    }

   // @SaCheckPermission("APPLICATION:CREATE")
    @PostMapping("/application")
    public R<ApplicationEntity> createApp(@RequestBody ApplicationEntity application) {
        return R.success(applicationService.createApp(application));
    }

    @SaCheckPermission("APPLICATION:CREATE")
    @PostMapping("/application/import")
    public R<Boolean> appImport(MultipartFile file) throws Exception {
        return R.status(applicationService.appImport(file));
    }

    //@SaCheckPermission("APPLICATION:CREATE")
    @PostMapping("/application/authentication")
    public R<String> authentication(@RequestBody JSONObject params) throws Exception {
        return R.success(applicationService.authentication(params));
    }

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{id}/function_lib")
    public R<List<FunctionLibEntity>> functionLib(@PathVariable("id") String id) {
        return R.success(applicationService.functionLib(id));
    }

  //  @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{id}/function_lib/{functionId}")
    public R<FunctionLibEntity> functionLib(@PathVariable("id") String id,@PathVariable("functionId") String functionId) {
        return R.success(applicationService.functionLib(id,functionId));
    }

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{id}/hit_test")
    public R<List<ParagraphVO>> hitTest(@PathVariable("id") String id, DataSearchDTO dto) {
        return R.success(applicationService.hitTest(id, dto));
    }

   // @SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("/application/{id}/edit_icon")
    public R<Boolean> editIcon(@PathVariable("id") String id, MultipartFile file) {
        return R.success(applicationService.editIcon(id, file));
    }

   // @SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("/application/{id}/publish")
    public R<Boolean> publish(@PathVariable("id") String id, @RequestBody JSONObject params) {
        return R.success(applicationService.publish(id, params));
    }

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/profile")
    public R<JSONObject> appProfile() {
        return R.success(applicationService.appProfile());
    }

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{id}/export")
    public void appExport(@PathVariable("id") String id,HttpServletResponse response) throws IOException {
        applicationService.appExport(id,response);
    }

    //@SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{page}/{size}")
    public R<IPage<ApplicationVO>> userApplications(@PathVariable("page") int page, @PathVariable("size") int size, Query query) {
        return R.success(applicationService.selectAppPage(page, size, query));
    }

    //@SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{id}")
    public R<ApplicationVO> getByAppId(@PathVariable("id") String id) {
        return R.success(applicationService.getAppById(id));
    }

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{id}/application/{appId}")
    public R<ApplicationVO> application(@PathVariable("id") String id,@PathVariable("appId") String appId) {
        return R.success(applicationService.getAppById(appId));
    }


    //@SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("/application/{id}")
    public R<Boolean> updateByAppId(@PathVariable("id") String id, @RequestBody ApplicationVO appVO) {
        return R.success(applicationService.updateAppById(id, appVO));
    }

   // @SaCheckPermission("APPLICATION:DELETE")
    @DeleteMapping("/application/{id}")
    public R<Boolean> deleteByAppId(@PathVariable("id") String id) {
        return R.success(applicationService.deleteByAppId(id));
    }

   // @SaCheckPermission("APPLICATION:READ")
    @PostMapping("/application/{appId}/play_demo_text")
    public ResponseEntity<byte[]> playDemoText(@PathVariable("appId") String appId, @RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        return new ResponseEntity<>(applicationService.playDemoText(appId, data), headers, HttpStatus.OK);
    }

    //@SaCheckPermission("APPLICATION:READ")
    @PostMapping("/application/{appId}/text_to_speech")
    public ResponseEntity<byte[]> textToSpeech(@PathVariable("appId") String appId, @RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mp3"));
        return new ResponseEntity<>(applicationService.textToSpeech(appId, data), headers, HttpStatus.OK);
    }

   // @SaCheckPermission("APPLICATION:READ")
    @PostMapping("/application/{appId}/speech_to_text")
    public  R<String> speechToText(@PathVariable("appId") String appId, MultipartFile file) throws IOException {
        return  R.data(applicationService.speechToText(appId,file));
    }


    //@SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{appId}/application")
    public R<List<ApplicationEntity>> listByUserId(@PathVariable("appId") String appId) {
        return R.success(applicationService.listByUserId(appId));
    }

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{appId}/access_token")
    public R<ApplicationAccessTokenEntity> getAccessToken(@PathVariable("appId") String appId) {
        return R.success(applicationService.getAccessToken(appId));
    }

    //@SaCheckPermission("APPLICATION:EDIT")
    @PutMapping("/application/{appId}/access_token")
    public R<ApplicationAccessTokenEntity> updateAccessToken(@PathVariable("appId") String appId, @RequestBody ApplicationAccessTokenDTO dto) {
        return R.success(applicationService.updateAccessToken(appId, dto));
    }

    //@SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{appId}/model")
    public R<List<ModelEntity>> model(@PathVariable("appId") String appId, String modelType) {
        return R.success(applicationService.getAppModels(appId, modelType));
    }

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{appId}/list_dataset")
    public R<List<DatasetEntity>> listDataset(@PathVariable("appId") String appId) {
        return R.success(applicationService.getDataset(appId));
    }

    //@SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{appId}/list_mcp")
    public R<List<McpLibEntity>> listMcp(@PathVariable("appId") String appId) {
        return R.success(applicationService.getMcp(appId));
    }

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{appId}/list_function")
    public R<List<FunctionLibEntity>> listFunction(@PathVariable("appId") String appId) {
        return R.success(applicationService.getFunction(appId));
    }

    //@SaCheckPermission("APPLICATION:CREATE")
    @PostMapping("/application/{appId}/dataset/{datasetId}/improve")
    public R<Boolean> improveChatLogs(@PathVariable("appId") String appId, @PathVariable("appId") String datasetId, ChatImproveDTO dto) {
        return R.success(applicationService.improveChatLogs(appId, dto));
    }

   // @SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/{appId}/model_params_form/{modelId}")
    public R<JSONArray> modelParams(@PathVariable("appId") String appId, @PathVariable("modelId") String modelId) {
        return R.success(applicationService.modelParams(appId, modelId));
    }

    //@SaCheckPermission("APPLICATION:READ")
    @GetMapping("/application/mcp_servers")
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
