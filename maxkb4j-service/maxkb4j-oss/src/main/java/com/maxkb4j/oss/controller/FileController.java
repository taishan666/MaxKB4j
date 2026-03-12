package com.maxkb4j.oss.controller;

import com.maxkb4j.common.api.R;
import com.maxkb4j.common.domain.dto.OssFile;
import com.maxkb4j.oss.service.MongoFileService;
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


    @PostMapping(value = {
            "/admin/api/oss/file",
            "/chat/api/oss/file"
    })
    public R<String> uploadFile(MultipartFile file) throws IOException {
        OssFile chatFile = mongoFileService.uploadFile(file);
        return R.success(chatFile.getUrl());
    }

    @GetMapping({
            "/admin/application/*/*/oss/file/{fileId:[\\w-]+}",
            "/admin/application/*/*/*/oss/file/{fileId:[\\w-]+}",
            "/admin/knowledge/*/*/oss/file/{fileId:[\\w-]+}",
            "/admin/oss/file/{fileId:[\\w-]+}",
            "/chat/oss/file/{fileId:[\\w-]+}",
            "/oss/file/{fileId:[\\w-]+}"})
    public void getFile(@PathVariable("fileId") String fileId, HttpServletResponse response){
        mongoFileService.getFile(fileId, response);
    }


}
