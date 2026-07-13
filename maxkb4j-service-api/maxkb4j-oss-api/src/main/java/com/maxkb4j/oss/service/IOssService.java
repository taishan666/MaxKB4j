package com.maxkb4j.oss.service;

import com.maxkb4j.common.domain.dto.OssFile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface IOssService {

    String uploadAndGetFileUrl(MultipartFile file) throws IOException;

    default String uploadAndGetFileUrl(String fileName, byte[] fileBytes){
        return uploadFile(fileName, fileBytes).getUrl();
    }

    OssFile uploadFile(String fileName, byte[] fileBytes);

    String storeFile(MultipartFile file) throws IOException;

    String storeFile(byte[] bytes,String fileName,String contentType);

    void downloadFile(String id,HttpServletResponse response);

    OssFile getFile(String id);

    InputStream getStream(String fileId) throws IOException;

    byte[] getBytes(String fileId);
}
