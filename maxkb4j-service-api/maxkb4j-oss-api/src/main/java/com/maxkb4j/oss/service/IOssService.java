package com.maxkb4j.oss.service;

import com.maxkb4j.common.domain.dto.OssFile;

import java.io.IOException;
import java.io.InputStream;

public interface IOssService {

    default String uploadAndGetFileUrl(String fileName, byte[] fileBytes) {
        return uploadFile(fileName, fileBytes).getUrl();
    }

    OssFile uploadFile(String fileName, byte[] fileBytes);

    String storeFile(byte[] bytes, String fileName, String contentType);

    OssFile getFile(String id);

    InputStream getStream(String fileId) throws IOException;

    byte[] getBytes(String fileId);
}
