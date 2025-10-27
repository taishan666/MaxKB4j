package com.tarzan.maxkb4j.module.application.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.util.McpToolUtil;
import com.tarzan.maxkb4j.module.application.domian.dto.ApplicationAccessTokenDTO;
import com.tarzan.maxkb4j.module.application.domian.dto.ApplicationQuery;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationAccessTokenEntity;
import com.tarzan.maxkb4j.module.application.domian.entity.ApplicationEntity;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationListVo;
import com.tarzan.maxkb4j.module.application.domian.vo.ApplicationVO;
import com.tarzan.maxkb4j.module.application.domian.vo.McpToolVO;
import com.tarzan.maxkb4j.module.application.service.ApplicationService;
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
@RequestMapping(AppConst.ADMIN_API+"/workspace/default")
@AllArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/application")
    public R<List<ApplicationListVo>> listApps(String folderId) {
        return R.success(applicationService.listApps(folderId));
    }

    @PostMapping("/application")
    public R<ApplicationEntity> createApp(@RequestBody ApplicationEntity application) {
        return R.success(applicationService.createApp(application));
    }

    @PostMapping("/application/import")
    public R<Boolean> appImport(MultipartFile file) throws Exception {
        return R.status(applicationService.appImport(file));
    }


    @PutMapping("/application/{id}/publish")
    public R<Boolean> publish(@PathVariable("id") String id, @RequestBody JSONObject params) {
        return R.success(applicationService.publish(id, params));
    }

    @GetMapping("/application/{id}/export")
    public void appExport(@PathVariable("id") String id,HttpServletResponse response) throws IOException {
        applicationService.appExport(id,response);
    }

    @GetMapping("/application/{current}/{size}")
    public R<IPage<ApplicationVO>> userApplications(@PathVariable("current") int current, @PathVariable("size") int size, ApplicationQuery query) {
        return R.success(applicationService.selectAppPage(current, size, query));
    }

    @GetMapping("/application/{id}")
    public R<ApplicationVO> getByAppId(@PathVariable("id") String id) {
        return R.success(applicationService.getDetail(id));
    }


    @PutMapping("/application/{id}")
    public R<Boolean> updateByAppId(@PathVariable("id") String id, @RequestBody ApplicationVO appVO) {
        return R.success(applicationService.updateAppById(id, appVO));
    }

    @DeleteMapping("/application/{id}")
    public R<Boolean> deleteByAppId(@PathVariable("id") String id) {
        return R.success(applicationService.deleteByAppId(id));
    }

    @PostMapping("/application/{appId}/play_demo_text")
    public ResponseEntity<byte[]> playDemoText(@PathVariable("appId") String appId, @RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/wav"));
        return new ResponseEntity<>(applicationService.playDemoText(appId, data), headers, HttpStatus.OK);
    }

    @PostMapping("/application/{appId}/text_to_speech")
    public ResponseEntity<byte[]> textToSpeech(@PathVariable("appId") String appId, @RequestBody JSONObject data) {
        // 设置 HTTP 响应头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mp3"));
        return new ResponseEntity<>(applicationService.textToSpeech(appId, data), headers, HttpStatus.OK);
    }

    @PostMapping("/application/{appId}/speech_to_text")
    public  R<String> speechToText(@PathVariable("appId") String appId, MultipartFile file) throws IOException {
        return  R.data(applicationService.speechToText(appId,file));
    }


    @GetMapping("/application/{appId}/access_token")
    public R<ApplicationAccessTokenEntity> getAccessToken(@PathVariable("appId") String appId) {
        return R.success(applicationService.getAccessToken(appId));
    }

    @PutMapping("/application/{appId}/access_token")
    public R<ApplicationAccessTokenEntity> updateAccessToken(@PathVariable("appId") String appId, @RequestBody ApplicationAccessTokenDTO dto) {
        return R.success(applicationService.updateAccessToken(appId, dto));
    }

    @PostMapping("/application/{appId}/mcp_tools")
    public R<List<McpToolVO>> mcpTools(@PathVariable("appId") String appId, @RequestBody JSONObject mcpServers) {
        System.out.println("mcpTools "+mcpServers);
        JSONObject mcpServersJson=JSONObject.parseObject(mcpServers.getString("mcpServers"));
        return R.data(McpToolUtil.getToolVos(mcpServersJson));
    }





}
