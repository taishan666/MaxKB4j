package com.tarzan.maxkb4j.module.oss.controller;

import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.core.workflow.model.ChatFile;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author tarzan
 * @date 2025-01-21 09:34:51
 */
@RestController
@RequiredArgsConstructor
public class FileController {

    private final MongoFileService mongoFileService;


    @PostMapping(value = "/{prefix}/api/oss/file")
    public R<String> uploadFile(@PathVariable("prefix") String prefix, MultipartFile file) throws IOException {
        ChatFile chatFile = mongoFileService.uploadFile(file);
        return R.success(chatFile.getUrl());
    }


    @GetMapping(value = "admin/application/workspace/{appId}/oss/file/{id}")
    public void getAppFile(@PathVariable("appId") String appId, @PathVariable("id") String id, HttpServletResponse response) {
        mongoFileService.getFile(id, response);
    }


    @GetMapping(value = "/{prefix}/oss/file/{id}")
    public void getFile(@PathVariable String prefix, @PathVariable("id") String id, HttpServletResponse response) {
        mongoFileService.getFile(id, response);
    }


}
