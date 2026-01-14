package com.tarzan.maxkb4j.module.application.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.aop.SaCheckPerm;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.util.McpToolUtil;
import com.tarzan.maxkb4j.module.application.domain.dto.*;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.domain.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationListVO;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationStatisticsVO;
import com.tarzan.maxkb4j.module.application.domain.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domain.vo.McpToolVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
import com.tarzan.maxkb4j.module.system.user.enums.PermissionEnum;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author tarzan
 * @date 2024-12-25 13:09:54
 */
@Tag(name = "APP应用管理", description = "APP应用管理")
@RestController
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @GetMapping("/application")
    public R<List<ApplicationListVO>> listApps(String folderId) {
        return R.success(applicationService.listApps(folderId));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_CREATE)
    @PostMapping("/application")
    public R<ApplicationEntity> createApp(@RequestBody ApplicationDTO application) {
        return R.success(applicationService.createApp(application));
    }

    @PostMapping("/application/folder/{folderId}/import")
    public R<Boolean> appImport(@PathVariable String folderId,MultipartFile file) throws Exception {
        return R.status(applicationService.appImport(file));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_EDIT)
    @PutMapping("/application/{id}/publish")
    public R<Boolean> publish(@PathVariable("id") String id, @RequestBody JSONObject params) {
        return R.success(applicationService.publish(id, params));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_EXPORT)
    @GetMapping("/application/{id}/export")
    public void appExport(@PathVariable("id") String id,HttpServletResponse response) throws IOException {
        applicationService.appExport(id,response);
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @GetMapping("/application/{current}/{size}")
    public R<IPage<ApplicationVO>> userApplications(@PathVariable("current") int current, @PathVariable("size") int size, ApplicationQuery query) {
        return R.success(applicationService.selectAppPage(current, size, query));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @GetMapping("/application/{id}")
    public R<ApplicationVO> getByAppId(@PathVariable("id") String id) {
        return R.success(applicationService.getDetail(id));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_EDIT)
    @PutMapping("/application/{id}")
    public R<Boolean> updateByAppId(@PathVariable("id") String id, @RequestBody ApplicationVO appVO) {
        return R.success(applicationService.updateAppById(id, appVO));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_DELETE)
    @DeleteMapping("/application/{id}")
    public R<Boolean> deleteByAppId(@PathVariable("id") String id) {
        return R.success(applicationService.deleteByAppId(id));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @PostMapping("/application/{id}/play_demo_text")
    public ResponseEntity<byte[]> playDemoText(@PathVariable("id") String id, @RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        return new ResponseEntity<>(applicationService.playDemoText(id, data), headers, HttpStatus.OK);
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @PostMapping("/application/{id}/text_to_speech")
    public ResponseEntity<byte[]> textToSpeech(@PathVariable("id") String id, @RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mp3"));
        return new ResponseEntity<>(applicationService.textToSpeech(id, data), headers, HttpStatus.OK);
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @PostMapping("/application/{id}/speech_to_text")
    public  R<String> speechToText(@PathVariable("id") String id, MultipartFile file) throws IOException {
        return  R.data(applicationService.speechToText(id,file));
    }


    @SaCheckPerm(PermissionEnum.APPLICATION_ACCESS_READ)
    @GetMapping("/application/{id}/access_token")
    public R<ApplicationAccessTokenEntity> getAccessToken(@PathVariable("id") String id) {
        return R.success(applicationService.getAccessToken(id));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_ACCESS_EDIT)
    @PutMapping("/application/{id}/access_token")
    public R<ApplicationAccessTokenEntity> updateAccessToken(@PathVariable("id") String id, @RequestBody ApplicationAccessTokenDTO dto) {
        return R.success(applicationService.updateAccessToken(id, dto));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_EDIT)
    @PostMapping(path = "application/{id}/model/{modelId}/prompt_generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String,String>> promptGenerate(@PathVariable String id, @PathVariable String modelId, @RequestBody PromptGenerateDTO dto){
        return applicationService.promptGenerate(id,modelId,dto);
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @GetMapping("/application/{id}/application_stats")
    public R<List<ApplicationStatisticsVO>> applicationStats(@PathVariable("id") String id, ChatQueryDTO query) {
        return R.success(applicationService.applicationStats(id, query));
    }

    @SaCheckPerm(PermissionEnum.APPLICATION_READ)
    @PostMapping("/application/{id}/mcp_tools")
    public R<List<McpToolVO>> mcpTools(@PathVariable("id") String id, @RequestBody JSONObject mcpServers) {
        JSONObject mcpServersJson=JSONObject.parseObject(mcpServers.getString("mcpServers"));
        return R.data(McpToolUtil.getToolVos(mcpServersJson));
    }






}
