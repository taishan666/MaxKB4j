package com.tarzan.maxkb4j.module.oss.controller;

import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.core.workflow.model.SysFile;
import com.tarzan.maxkb4j.module.oss.service.MongoFileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
        SysFile chatFile = mongoFileService.uploadFile(file);
        return R.success(chatFile.getUrl());
    }

    @GetMapping({
            "/admin/application/*/*/oss/file/{fileId:[\\w-]+}",
            "/admin/application/*/*/*/oss/file/{fileId:[\\w-]+}",
            "/admin/knowledge/*/*/oss/file/{fileId:[\\w-]+}",
            "/chat/oss/file/{fileId:[\\w-]+}",
            "/oss/file/{fileId:[\\w-]+}"})
    public void getFile(@PathVariable("fileId") String fileId, HttpServletResponse response){
        mongoFileService.getFile(fileId, response);
    }


}
