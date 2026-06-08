package com.maxkb4j.knowledge.service;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.util.IoUtil;
import com.maxkb4j.knowledge.entity.DocumentEntity;
import com.maxkb4j.oss.service.IOssService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentSourceFileService {

    private static final String META_SOURCE_FILE_ID = "sourceFileId";
    private static final String META_ALLOW_DOWNLOAD = "allow_download";

    private final DocumentService documentService;
    private final IOssService ossService;

    public boolean download(String docId, HttpServletResponse response) throws IOException {
        DocumentEntity doc = documentService.getById(docId);
        if (doc == null || doc.getMeta() == null) return false;

        String fileId = doc.getMeta().getString(META_SOURCE_FILE_ID);
        if (StringUtils.isBlank(fileId)) return false;

        try (InputStream in = ossService.getStream(fileId)) {
            IoUtil.copy(in, response.getOutputStream());
            return true;
        }
    }

    public boolean replace(String docId, MultipartFile file) throws IOException {
        DocumentEntity doc = documentService.getById(docId);
        if (doc == null) return false;
        String fileId = ossService.storeFile(file);
        doc.setMeta(new JSONObject(Map.of(META_ALLOW_DOWNLOAD, true, META_SOURCE_FILE_ID, fileId)));
        return documentService.updateById(doc);
    }
}
